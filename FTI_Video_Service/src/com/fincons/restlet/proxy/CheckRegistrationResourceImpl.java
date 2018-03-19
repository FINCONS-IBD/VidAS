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
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fincons.utils.PropertiesHelper;

public class CheckRegistrationResourceImpl extends ServerResource {

	final static Logger logger = Logger.getLogger(CheckRegistrationResourceImpl.class);

	@Get
	public Representation proxyCheckRegistration() {
		logger.trace("Called the CheckRegistrationResourceImpl proxyCheckRegistration method...");
		
		String message ="";
		JSONObject response = null;
		String textResponse = "";

		try {
			Form queryParams = getRequest().getResourceRef().getQueryAsForm(); 
			String username = queryParams.getFirstValue("username"); 

			logger.info("Called the proxyCheckRegistration method with following username: " + username);

			if(username.isEmpty()){						
				logger.info("Json not valid! Empty username...");

				int status = 500;
				message = "Internal Server Error";

				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", status);
				jsonObjectRet.put("message", message);

			    Representation rep = new JsonRepresentation(jsonObjectRet);
			    getResponse().setEntity(rep);
				getResponse().setStatus(new Status(status));

				return rep;
			}

			logger.debug("Prepare the client for the remote call to check the registration...");
			Client client = new Client(new Context(), Protocol.HTTP);
			ClientResource clientResource = new ClientResource(PropertiesHelper.getProps().getProperty("getCheckRegistrationServicePath"));
			clientResource.getReference().addQueryParameter("username", username);
			clientResource.setNext(client);		

			Representation serverResponse = null;
			try{

				logger.info("Invoking the getCheckRegistrationServicePath with the following parameters: " + username);

				serverResponse = clientResource.get();
				textResponse = serverResponse.getText();
				response = new JSONObject(textResponse);

				int status = response.has("code")?response.getInt("code"):response.getInt("faultCode");
				
				logger.info("getCheckRegistrationServicePath service response: status " + status);
				
				getResponse().setStatus(new Status(status));

			}catch(ResourceException re){
				Representation error = clientResource.getResponseEntity();
				logger.error(re.getStatus()+ " "+ re.getMessage());
				if(error!=null){
					textResponse = error.getText();
					response = new JSONObject(textResponse);
				}
				getResponse().setStatus(re.getStatus());

				logger.error("Internal Server error: " + textResponse, re);
			}

		} catch (Exception e) {
			logger.error("Internal Server Error", e);
			e.printStackTrace();

			int status = 500;
			getResponse().setStatus(new Status(status));

		}

	    Representation rep = new JsonRepresentation(response);
	    getResponse().setEntity(rep);

		return rep;
	}


}
