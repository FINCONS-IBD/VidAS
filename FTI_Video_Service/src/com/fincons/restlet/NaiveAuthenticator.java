package com.fincons.restlet;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Form;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;
import org.restlet.util.Series;

import com.fincons.utils.PropertiesHelper;

/**
 * Naive implementation of a cookie authenticator. The cookie is called
 * "Credentials" and contains "UTCDATE<<>>USERNAME+AES_ENCRYPTED_USERNAME"
 */
public class NaiveAuthenticator extends ChallengeAuthenticator {

	final static Logger logger = Logger.getLogger(NaiveAuthenticator.class);

	public NaiveAuthenticator(Context context, boolean optional, String realm) {
		super(context, optional, ChallengeScheme.HTTP_BASIC, realm);
	}

	public NaiveAuthenticator(Context context, boolean optional, String realm, Verifier verifier) {
		super(context, optional, ChallengeScheme.HTTP_BASIC, realm, verifier);
	}

	public NaiveAuthenticator(Context context, String realm) {
		super(context, ChallengeScheme.HTTP_BASIC, realm);
	}

	@Override
	protected int beforeHandle(Request request, Response response) {
		if (Method.POST.equals(request.getMethod()) && request.getResourceRef().getPath()
				.compareTo(PropertiesHelper.getProps().getProperty("loginFormAction")) == 0) {

			// LOGIN FORM SUBMISSION
			logger.trace("Called beforeHandle restlet method...");

			logger.info("Intercepted a LOGIN Form submission...");
			Form dataform = new Form(request.getEntity());
			String identifier = dataform.getFirstValue("identifier");
			String JSONResponseToken = dataform.getFirstValue("JSONResponseToken");

			request.setChallengeResponse(
					new ChallengeResponse(ChallengeScheme.HTTP_BASIC, identifier, JSONResponseToken));

			request.setMethod(Method.GET);
		} else {
			logger.info("Intercepted an invocation of a protected resource, check if the JWT token is present...");
			Series headers = (Series) request.getAttributes().get("org.restlet.http.headers");
			String user_token = headers.getFirstValue("authorization");

			if (user_token != null && !user_token.isEmpty()) {
				logger.info("The JWT token is present, continue the auth process...");
				return CONTINUE;
			} else {
				logger.info("JWT token not present, user unauthorized!");
				response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			}
		}

		return super.beforeHandle(request, response);

	}

	@Override
	protected void afterHandle(Request request, Response response) {

		logger.trace("Called afterHandle restlet method...");

		super.afterHandle(request, response);

		if (request.getClientInfo().isAuthenticated()) {
		}

	}

	@Override
	public void challenge(Response response, boolean stale) {

		logger.trace("Called challenge restlet method to load the login form page...");

		// Load the FreeMarker template
		Representation ftl = new ClientResource(
				LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/templates/index.html")).get();
		String authentication_failed = "false";

		// load the auth_failed flag (setted by the custom validator)
		// to intercept a login failed request and show an html message in the
		// page
		if (response.getAttributes() != null) {
			authentication_failed = (String) response.getAttributes().get("auth_failed");

			if (authentication_failed == null)
				authentication_failed = "false";
		}

		logger.info("Wraps the bean with a FreeMarker representation to show the login html page...");
		Map<String, Object> dataModel = new HashMap<String, Object>();
		dataModel.put("authentication_failed", "" + authentication_failed);
		dataModel.put("proxyGetUserDataServicePath",
				PropertiesHelper.getProps().getProperty("proxyGetUserDataServicePath"));

		// Wraps the bean with a FreeMarker representation
		response.setEntity(new TemplateRepresentation(ftl, dataModel, MediaType.TEXT_HTML));

		response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
	}

}
