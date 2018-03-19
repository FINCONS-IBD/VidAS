package com.fincons.restlet.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import org.apache.commons.codec.binary.Base64;
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
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import com.fincons.utils.Constants;
import com.fincons.utils.CpabeManager;
import com.fincons.utils.PropertiesHelper;

public class DecryptionServerResource extends ServerResource {

	final static Logger logger = Logger.getLogger(DecryptionServerResource.class);
	
	@Post
	public Representation decryptProxy(String parameters) {
		
		logger.info("Called the decryptProxy method to decrypt video file");
		String publicKey="";
		String personalKey="";
		String nameFile="";
		try {
			JSONObject obj = new JSONObject(parameters);
			publicKey=obj.optString("publicKey");
			personalKey=obj.optString("personalKey");
			nameFile=obj.optString("nameFile");
		} catch (JSONException e1) {
			logger.error("Error Reading parameter" , e1);
			getResponse().setStatus(new Status(400));
			return new JsonRepresentation(new JSONObject());
		}
		

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
	
			
			
			clientResource = new ClientResource(url);
			Series<Header> requestHeaders = new Series(Header.class); 
			requestHeaders.add(new Header("Authorization", user_token)); //N.B. the add accept only standard HTTP Headers
			clientResource.getRequestAttributes().put("org.restlet.http.headers", requestHeaders); 
			
			clientResource.setNext(client);
			
			Representation serverResponse =  null;
		
			clientResource.setMethod(Method.GET);
			serverResponse = clientResource.get();
			
			SecureRandom secure = new SecureRandom();
			Base64 b64=new Base64(true);

			String pathFile = DecryptionServerResource.class.getClassLoader().getResource(PropertiesHelper.getProps().getProperty(Constants.PATH_FILE))
					.getPath()+nameFile.split("/")[1];
		
			File fileEncrypt=new File(pathFile);
			
			JSONObject jsonResponse= new JSONObject();
			try {
				InputStream iSFileEncrypt= serverResponse.getStream();
				System.out.println(iSFileEncrypt.available());
				
				try(OutputStream oos = new FileOutputStream(fileEncrypt)){
					byte[] buf = new byte[iSFileEncrypt.available()];
					int c = 0;
					while ((c = iSFileEncrypt.read(buf, 0, buf.length)) > 0) {
						oos.write(buf, 0, c);
						oos.flush();
					}
					oos.close();
				} catch (IOException e) {
					logger.info("IOException: "+e);
					getResponse().setStatus(new Status(500));
					return new JsonRepresentation(getJsonError(500, "Internal Server Error"));
				}
				
			} catch (IOException e) {
				logger.info("IOException: "+e);
				getResponse().setStatus(new Status(500));
				return new JsonRepresentation(getJsonError(500, "Internal Server Error"));
			}
						
			String extension ="file";
			String[] splittedForExtension=nameFile.split("\\.");
			if(splittedForExtension.length>2){
				extension=splittedForExtension[splittedForExtension.length-2];
			}
			String nameFileDecrypt= b64.encodeToString(secure.generateSeed(16)).replace("-","_").trim()+"."+extension;
			logger.info("pathFile: |"+nameFile+"|");
			String pathFileDecrypted= DecryptionServerResource.class.getClassLoader().getResource(PropertiesHelper.getProps().getProperty(Constants.PATH_FILE)).getPath()+nameFileDecrypt;
			
			try {
				logger.debug("Start Decryption");	
				CpabeManager cpabeManager = new CpabeManager();
				boolean isDecrypted=cpabeManager.dec(publicKey, personalKey, pathFile, pathFileDecrypted);
				if(!isDecrypted){
					jsonResponse.put("code", 406);
					jsonResponse.put("message","Your user's profile does not match the file access policy");
				}else{
					jsonResponse.put("code", 200);
					jsonResponse.put("url", PropertiesHelper.getProps().getProperty(Constants.PATH_FILE)+"/"+nameFileDecrypt);
				}
			} catch (JSONException e) {
				logger.info("JSONException: "+e);
				getResponse().setStatus(new Status(500));
				return new JsonRepresentation(getJsonError(500, "Error in JSON parsing"));
			} catch (Exception e1) {
				logger.info("Decryption Fail: "+e1);
				getResponse().setStatus(new Status(406));
				return new JsonRepresentation(getJsonError(406, "Your user's profile does not match the file access policy"));
			} 
			
			
			return new JsonRepresentation(jsonResponse);
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