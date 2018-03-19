package com.fincons.token.restlet;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fincons.h2.H2DatabaseOperation;


public class LogoutResource extends ServerResource {
	final static Logger logger = Logger.getLogger(LogoutResource.class);

	@Post
	public Representation logout(String parameters){
		
		logger.debug("Called the logout method..." + parameters);
		
		JSONObject jsonResponse = new JSONObject();
		int status = 500; 
		String message = "Internal server error";
//		final Form form = new Form(parameters);
//		String username = form.getFirstValue("username");
		String username="";
		JSONObject obj = null;
		try {
			obj = new JSONObject(parameters);
			username=obj.optString("username");
			
			if(username!=null){
				logger.info("Try to perform logout for the user: " + username);
				boolean success_flag = H2DatabaseOperation.deleteUserFromUsername(username);
				
				if(success_flag){
					status = 200;
					message = "Logout succesful";
				}else
					logger.debug("Problems during the H2 database user deletion!");
			}else
				logger.debug("Impossible to perform the logout on an empry username!");
		} catch (JSONException e1) {
			message="Parameters invalid";
			logger.error("JSONException during parameter reading!", e1);
			e1.printStackTrace();
		}
		try {
			jsonResponse.put("code", status);
			jsonResponse.put("message", message);
		} catch (JSONException e) {
			logger.error("JSONException during jsonResponse creation!", e);
			e.printStackTrace();
		}
		
		logger.info("Logout completed for the user " + username + 
				" with the following response: " + jsonResponse.toString());
		
		getResponse().setStatus(new Status(status));
		return new JsonRepresentation(jsonResponse);
	}

}
