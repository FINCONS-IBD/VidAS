package com.fincons.restlet.proxy;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;
import org.json.JSONException;
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
import org.restlet.resource.ServerResource;

import com.fincons.utils.Constants;
import com.fincons.utils.CryptUtil;
import com.fincons.utils.PropertiesHelper;

public class UserDataResourceImpl extends ServerResource  {

	final static Logger logger = Logger.getLogger(UserDataResourceImpl.class);

	@Get
	public Representation getUserData() {

		logger.trace("Called the getUserData method...");

		int status = 0;
		String message ="";
		JSONObject response =  new JSONObject();
		Representation serverResponse = null;
		try {

			Form queryParams = getRequest().getResourceRef().getQueryAsForm(); 
			String username = queryParams.getFirstValue("username"); 

			logger.info("The user " + username + " try to retrieve the user information from the LDAP service...");

			if(username.isEmpty()){						
				logger.error("Json not valid! Empty username...");

				status = 500;
				message = "Internal Server Error";

				getResponse().setStatus(new Status(status));

				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", status);
				jsonObjectRet.put("message", message);

				return new JsonRepresentation(jsonObjectRet);
			}

			logger.info("Prepare the client for the remote call to performe the getUserData...");
			Client client = new Client(new Context(), Protocol.HTTP);
			ClientResource clientResource = new ClientResource(PropertiesHelper.getProps().getProperty("getTokenServiceUserDataServicePath"));
			clientResource.setNext(client);	
			JSONObject usernameJson=new JSONObject().put("username", username); 
			serverResponse = clientResource.post(usernameJson.toString());

			String textResponse = serverResponse.getText();
			JSONObject responseTokenService = new JSONObject(textResponse);
			status=responseTokenService.optInt("code");
			
			logger.info("getUserData service response returned successful with status " + status);
			
			if(status==200){
				String HMACloginUserData= responseTokenService.optString("HMACloginUserData");
				String signature=CryptUtil.getHMAC(PropertiesHelper.getProps().getProperty(Constants.HMAC_KEY),responseTokenService.optJSONObject("loginUserData").toString());
				if(HMACloginUserData.equals(signature)){
					logger.info("HMAC Check OK for this getUserData request...");
					response=responseTokenService;
					response.remove("HMACloginUserData");
				}else{
					logger.info("HMAC Check Failed for this getUserData request...");
					status= 400;
				}
			}
			
			logger.debug("getUserData process completed with the following response: " + response);
			
		} catch (JSONException e) {
			logger.error("Internal server error", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(response);
		} catch (IOException e) {
			logger.error("Internal server error", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(response);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		getResponse().setStatus(new Status(status));

		return new JsonRepresentation(response);
	}

}
