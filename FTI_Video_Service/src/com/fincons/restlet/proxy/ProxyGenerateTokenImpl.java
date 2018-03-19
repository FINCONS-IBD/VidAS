package com.fincons.restlet.proxy;

import org.apache.log4j.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import com.fincons.utils.PropertiesHelper;

public class ProxyGenerateTokenImpl extends ServerResource  {

	final static Logger logger = Logger.getLogger(ProxyGenerateTokenImpl.class);

	@Get
	public Representation generateToken() throws ResourceException {

		logger.trace("Called the ProxyGenerateTokenImpl generateToken method...");
		
		ClientResource clientResource = null;

		Client client = new Client(new Context(), Protocol.HTTP);
		
		//move the Auth JWT Token from the first request to second request
		Series<Header> firs_headers =  (Series<Header>) getRequestAttributes().get("org.restlet.http.headers");
		String user_token = firs_headers.getFirstValue("authorization");
		
		
		
		
		JwtConsumer jwtConsumerSkipSign = new JwtConsumerBuilder()
		        .setDisableRequireSignature()
		        .setSkipSignatureVerification()
		        .build();
		JwtClaims jwtClaims;
		JSONObject jsonClaims;
		try{
			jwtClaims = jwtConsumerSkipSign.processToClaims(user_token);
			jsonClaims = new JSONObject(jwtClaims.toJson());
			JSONObject actionJSON = jsonClaims.optJSONObject("action");
			String url = actionJSON.optString("url");
			if(url.equals("_")){
				clientResource = new ClientResource(PropertiesHelper.getProps().getProperty("defaultPolicyTokenServicePath"));
			} else{
				clientResource = new ClientResource(PropertiesHelper.getProps().getProperty("generateTokenServicePath"));
			}
		}catch (InvalidJwtException e) {
			logger.error("Invalid JWT, Request Not Valid ", e);
			getResponse().setStatus(new Status(403));
			return new JsonRepresentation(getJsonError(403, "Token Timeout"));
		} catch (JSONException e) {
			logger.fatal("Internal Server Error, Error in JSON Parser ", e);
			getResponse().setStatus(new Status(500));			
			return new JsonRepresentation(getJsonError(500, "Error in JSON Parser : "+e.getMessage()));
		}
		
		
		Series<Header> requestHeaders = new Series(Header.class); 
		requestHeaders.add(new Header("Authorization", user_token)); //N.B. the add accept only standard HTTP Headers
		clientResource.getRequestAttributes().put("org.restlet.http.headers", requestHeaders); 
		
		logger.info("Moved the Auth JWT Token from the first request to second request (also empty or null)");
		
		clientResource.setNext(client);

		Representation serverResponse = clientResource.get();
		return serverResponse;
	}
	
	public static JSONObject getJsonError(int code, String message){
		JSONObject errorJson= new JSONObject();
		try {
			errorJson.put("code", code);
			errorJson.put("message", message);
		} catch (JSONException e) {
			logger.error("Error in create ErrorJson code:"+code +"message:'"+message+"'.",e );
		}finally{
			return errorJson;
		}
	}
}
