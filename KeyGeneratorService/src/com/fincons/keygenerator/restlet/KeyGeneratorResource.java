package com.fincons.keygenerator.restlet;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import com.fincons.keygenerator.restlet.ldap.LdapManager;
import com.fincons.keygenerator.utils.CpabeManager;

public class KeyGeneratorResource extends ServerResource {

	final static Logger logger = Logger.getLogger(KeyGeneratorResource.class);
	
	@Get
	public JsonRepresentation generateKeys() {

		logger.info("Called the generateKeys method");
		
		Series headers = (Series) getRequest().getAttributes().get("org.restlet.http.headers");
		String user_token = headers.getFirstValue("authorization");

		JwtConsumer jwtConsumerSkipSign = new JwtConsumerBuilder()
        .setDisableRequireSignature()
        .setSkipSignatureVerification()
        .build();
		
		
//		String username=getRequest().getClientInfo().getUser().getIdentifier();
		JSONObject jsonObjectRet = null;

		JSONObject result = null;
		
		try {
			JwtClaims jwtExternalClaims = jwtConsumerSkipSign.processToClaims(user_token);
			String username=jwtExternalClaims.getSubject();
			try{
				result = LdapManager.searchUser(new JSONObject().put("mail", username));
			}catch(NamingException ne){
				logger.error("Error in LDAP", ne);
				jsonObjectRet.put("code", 500);
				jsonObjectRet.put("message", "LDAP Service not available or timeout received");
				logger.info("LDAP Service not available or timeout received");
				return new JsonRepresentation(jsonObjectRet);
			}
			if(result.getInt("code") != 200){
				jsonObjectRet = new JSONObject();
				try {
					jsonObjectRet.put("faultCode", Status.CLIENT_ERROR_NOT_FOUND);
					jsonObjectRet.put("message", "Username Not Found");
				} catch (JSONException e1) {
					logger.error("JSONException", e1);
					e1.printStackTrace();
				}
				
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				return new JsonRepresentation(jsonObjectRet);
			}

		} catch (JSONException je) {
			logger.error("JSONException", je);
			je.printStackTrace();			
		} catch (InvalidJwtException ije) {
			logger.error("InvalidJwtException", ije);
			ije.printStackTrace();		
		} catch (MalformedClaimException mce) {
			logger.error("MalformedClaimException", mce);
			mce.printStackTrace();
		}
		
		

		String jsonKeys = null;
		int status;
		try {
			
			String user_attributes = result.getString("user_attributes");
			jsonKeys = CpabeManager.keygen(user_attributes);

			jsonObjectRet = new JSONObject(jsonKeys);
			status = jsonObjectRet.has("code")?jsonObjectRet.getInt("code"):jsonObjectRet.getInt("faultCode");
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
			jsonObjectRet = new JSONObject();
			try {
				jsonObjectRet.put("faultCode", Status.SERVER_ERROR_INTERNAL);
				jsonObjectRet.put("message", "Internal Server Error");
			} catch (JSONException e1) {
				logger.error("JSONException", e1);
				e1.printStackTrace();
			}
			
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonObjectRet);
		}	

		getResponse().setStatus(new Status(status));
		logger.info("Key generation sucessfull completed.");
		return new JsonRepresentation(jsonObjectRet);

	}

}