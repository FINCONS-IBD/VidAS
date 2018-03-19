package com.fincons.videostorage.restlet;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Status;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;
import org.restlet.util.Series;

/**
 * Naive implementation of a cookie authenticator.
 * The cookie is called "Credentials" and contains "UTCDATE<<>>USERNAME+AES_ENCRYPTED_USERNAME"
 */
public class TokenAuthenticator extends ChallengeAuthenticator {

	final static Logger logger = Logger.getLogger(TokenAuthenticator.class);

	//	public boolean authentication_failed = false;

	public TokenAuthenticator(Context context, boolean optional,
			String realm) {
		super(context, optional, ChallengeScheme.HTTP_BASIC, realm);
	}

	public TokenAuthenticator(Context context, boolean optional,
			String realm, Verifier verifier) {
		super(context, optional, ChallengeScheme.HTTP_BASIC, realm, verifier);
	}

	public TokenAuthenticator(Context context, String realm) {
		super(context, ChallengeScheme.HTTP_BASIC, realm);
	}


	@Override
	protected int beforeHandle(Request request, Response response) {
		//THE LOGIN SERVICE CHECK IF THERE IS THE HTTP HEADER WITH THE JWT
		logger.info("Intercepted an invocation of a protected resource, check if the JWT token is present...");
		Series headers = (Series) request.getAttributes().get("org.restlet.http.headers");
		String user_token = headers.getFirstValue("authorization");

		if(user_token != null && !user_token.isEmpty()){
			
			request.setChallengeResponse(new ChallengeResponse(
					ChallengeScheme.HTTP_BASIC, "token" ,user_token));
//			request.setMethod(Method.GET);
			logger.info("The JWT token is present, continue the auth process...");
			
		}else{
			logger.info("JWT token not present, user unauthorized!");
			response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
		}
	
		return super.beforeHandle(request, response);

	}	

	@Override
	protected void afterHandle(Request request, Response response) {

		logger.trace("Called afterHandle restlet method...");

		super.afterHandle(request, response);

		if (request.getClientInfo().isAuthenticated()) {
			//NOW DO NOTHING, INSERT HERE THE CODE TO BE EXECUTE AFTER A LOGIN SUCCESFUL;
		}

	}


	@Override
	public void challenge(Response response, boolean stale) {
		
	}

}
