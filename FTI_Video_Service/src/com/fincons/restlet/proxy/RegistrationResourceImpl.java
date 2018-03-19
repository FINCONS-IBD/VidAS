package com.fincons.restlet.proxy;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fincons.utils.PropertiesHelper;

public class RegistrationResourceImpl extends ServerResource {

	final static Logger logger = Logger.getLogger(RegistrationResourceImpl.class);

	@Post
	public Representation proxyRegistration(Representation entity) {
		JSONObject response = null;

		logger.trace("Called the proxyRegistration method...");
		
		final Form form = new Form(entity);
		String jsonCert = form.getFirstValue("jsonCert");

		try {

			JSONObject obj = new JSONObject(jsonCert);
			String username = obj.optString("username");
			JSONObject certificate = obj.optJSONObject("certificate");
			String publicKey = certificate.optString("publicKey");
			String privateKey = certificate.optString("privateKey");

			logger.info("The user " + username + " try to perform the new account creation");
			
			if(username==null || username.isEmpty() 
					|| publicKey==null || publicKey.isEmpty()
					|| privateKey==null || privateKey.isEmpty()){

				logger.info("Json not valid! Empty username, privateKey or publicKey node...");

				int status = 500;
				String message = "Internal Server Error";

				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", status);
				jsonObjectRet.put("message", message);

				getResponse().setStatus(new Status(status));
				return new JsonRepresentation(jsonObjectRet);
			}

			logger.info("Valid JSON parameters! Prepare the client for the remote call to performe the registration...");
			Client client = new Client(new Context(), Protocol.HTTP);
			ClientResource clientResource = new ClientResource(PropertiesHelper.getProps().getProperty("postkRegistrationServicePath"));
			clientResource.setNext(client);

			Representation serverResponse = null;
			String textResponse = "";
			try{

				logger.info("Invoking the POST registration service...");

				serverResponse = clientResource.post(obj.toString());
				textResponse = serverResponse.getText();

				logger.info("Registration service response returned successful");
				
				response = new JSONObject(textResponse);

			}catch(ResourceException re){

				Representation error = clientResource.getResponseEntity();
				textResponse = "{\"code\":"+re.getStatus().getCode() + ",\"message\":\""+re.getMessage()+"\"}";

				response = new JSONObject(textResponse);
				logger.error("A remote error occours: " + textResponse, re);

			}

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("Internal Server Error",ex);
		}

		return new JsonRepresentation(response);
	}
}
