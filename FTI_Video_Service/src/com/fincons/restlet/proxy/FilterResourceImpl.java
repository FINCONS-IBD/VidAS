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
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import com.fincons.utils.PropertiesHelper;

public class FilterResourceImpl extends ServerResource  {

	final static Logger logger = Logger.getLogger(FilterResourceImpl.class);

	@Post
	public Representation filter(String parameters) throws ResourceException{
		

		logger.trace("Request filter Service");
		
		ClientResource clientResource = null;

		Client client = new Client(new Context(), Protocol.HTTP);
		
		//move the Auth JWT Token from the first request to second request
		Series<Header> firs_headers =  (Series<Header>) getRequestAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
		String user_token = firs_headers.getFirstValue("authorization");
		
		JwtConsumer jwtConsumerSkipSign = new JwtConsumerBuilder()
        .setDisableRequireSignature()
        .setSkipSignatureVerification()
        .build();
		String method="";
		String url="";
		try {
			JwtClaims jwtExternalClaims = jwtConsumerSkipSign.processToClaims(user_token);
			JwtClaims jwtInternalClaims = jwtConsumerSkipSign.processToClaims(jwtExternalClaims.getClaimValue("token").toString());
			JSONObject	jsonClaim = new JSONObject(jwtInternalClaims.toJson());
			JSONObject action= jsonClaim.optJSONObject("action");
			method= action.optString("method");
			String server= PropertiesHelper.getProps().getProperty(action.optString("service"));
			url= server+"/"+ action.optString("url");
		} catch (InvalidJwtException e) {
			logger.error("Invalid Token", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(401));
			return new JsonRepresentation(getJsonError(401, "Invalid Token"));
		} catch (JSONException e) {
			logger.error("Error Json", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(getJsonError(500, "Error in JSON parsing"));
		}
	
		logger.debug("Request forward: "+ url +" with Method"+ method);	
		
		clientResource = new ClientResource(url);
		Series<Header> requestHeaders = new Series(Header.class); 
		String contentType = firs_headers.getFirstValue("content-type");
		requestHeaders.add(new Header("Authorization", user_token)); //N.B. the add accept only standard HTTP Headers
		requestHeaders.add(new Header("Content-Type", contentType)); //N.B. the add accept only standard HTTP Headers
		
		clientResource.getRequestAttributes().put("org.restlet.http.headers", requestHeaders); 
		
		clientResource.setNext(client);

		Representation serverResponse =  null;
		
		switch (method){
			case "GET":
				clientResource.setMethod(Method.GET);
				serverResponse = clientResource.get();
				break;
			case "POST":
				clientResource.setMethod(Method.POST);
				serverResponse = clientResource.post(parameters);
				break;
			case "PUT":
				clientResource.setMethod(Method.PUT);
				serverResponse = clientResource.put(parameters);
				break;
			case "DELETE":
				clientResource.setMethod(Method.DELETE);
				serverResponse = clientResource.delete();
				break;
		}
		logger.info("Request forward");
		
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
