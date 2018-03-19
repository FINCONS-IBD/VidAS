package com.fincons.token.restlet;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.fincons.token.restlet.ldap.LdapManager;

public class CheckRegistrationResource extends ServerResource  {

	final static Logger logger = Logger.getLogger(CheckRegistrationResource.class);

	@Get
	public Representation getUserData() {
		
		logger.trace("Called the getUserData method...");
		
		int status = 0;
		String message ="";
		JSONObject capabilities = null;
		JSONObject jsonResponse =  new JSONObject();;
		try {
			Form queryParams = getRequest().getResourceRef().getQueryAsForm(); 
			String username = queryParams.getFirstValue("username"); 

			logger.info("Called the getUserData method with following parameters: " + username);
			
			if(username.isEmpty()){						
				logger.info("Json not valid! Empty username...");

				status = 500;
				message = "Internal Server Error";

				getResponse().setStatus(new Status(status));

				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", status);
				jsonObjectRet.put("message", message);

				logger.debug("The service returning code " + status + " and message \""+ message +"\"");
				return new JsonRepresentation(jsonObjectRet);
			}
			try{
				capabilities=LdapManager.searchUser(new JSONObject().put("mail", username));
			}catch(NamingException ne){
				logger.error("Error in LDAP", ne);
				status= 500;
				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", 500);
				jsonObjectRet.put("message", "LDAP Service not available or timeout received");
				logger.info("LDAP Service not available or timeout received");
				return new JsonRepresentation(jsonObjectRet);
			}
			
			status = capabilities.has("code")?capabilities.getInt("code"):500;
			if(status==200) 
				if(capabilities.opt("certificate").equals("null")){
					message="OK";
				}else {
					status=405;// se l'utente è presente ma con il campo description popolato
					message="Username not avaiable";
			}else{
				message="Username not avaiable";
			}
			jsonResponse.put("code", status);
			jsonResponse.put("message", message);
			
			logger.debug("The service returning code " + status + " and message \""+ message +"\"");
			
		} catch (JSONException e) {
			logger.error("Internal server error", e);
			getResponse().setStatus(new Status(500));
			logger.debug("The service returning code 500" + " and message \"Internal Server error\"");
			return new JsonRepresentation(jsonResponse);
		}

		getResponse().setStatus(new Status(status));

		return new JsonRepresentation(jsonResponse);
	}

}
