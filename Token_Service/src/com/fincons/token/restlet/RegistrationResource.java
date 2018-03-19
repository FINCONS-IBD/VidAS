package com.fincons.token.restlet;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fincons.token.restlet.ldap.LdapManager;

public class RegistrationResource  extends ServerResource  {

	final static Logger logger = Logger.getLogger(RegistrationResource.class);

	@Post
	public Representation registration(String parameters){
		logger.trace("Called the registration() method...");

		String message ="";
		JSONObject jsonResponse =  new JSONObject();
		try {
			JSONObject obj = new JSONObject(parameters);
			String username=obj.optString("username");
			JSONObject certificate=obj.optJSONObject("certificate");
			
			logger.info("The user " + username +" try to attempt the new account creation...");
			
			if(username.isEmpty()){						
				logger.info("Json not valid! Empty username. The service return status 500 and message \"Internal Server Error\"");

				int status = 500;
				message = "Internal Server Error";

				getResponse().setStatus(new Status(status));

				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", status);
				jsonObjectRet.put("message", message);

				return new JsonRepresentation(jsonObjectRet);
			}

			logger.info("Valid registration. The service try to save the certificate on the LDAP DS...");
						
			boolean esito=false;
			try{
			esito=LdapManager.setCertificate(username, certificate);
			}catch(NamingException ne){
				logger.error("Error in LDAP", ne);
				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", 500);
				jsonObjectRet.put("message", "LDAP Service not available or timeout received");
				logger.info("LDAP Service not available or timeout received");
				return new JsonRepresentation(jsonObjectRet);
			}
			
			int status = esito ? 200:500;
			if(status==200){
				message="OK";
			}else{
				message="Registration Failed";
			}
			jsonResponse.put("code", status);
			jsonResponse.put("message", message);
			
			getResponse().setStatus(new Status(status));	
			
			logger.info("Registration process completed with the code " + status + " and the message " + message);
		} catch (JSONException e) {
			logger.error("Internal server error", e);
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonResponse);
		}

		return new JsonRepresentation(jsonResponse);
	}
}