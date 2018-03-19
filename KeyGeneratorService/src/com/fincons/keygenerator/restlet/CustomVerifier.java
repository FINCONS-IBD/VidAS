package com.fincons.keygenerator.restlet;

import java.net.URLDecoder;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.codec.binary.Base64;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.security.SecretVerifier;

import com.fincons.keygenerator.utils.AESencrpPS;
import com.fincons.keygenerator.utils.Constants;
import com.fincons.keygenerator.utils.PropertiesHelper;

public class CustomVerifier extends SecretVerifier{

	/*
static int	RESULT_INVALID 
    Invalid credentials provided.
static int	RESULT_MISSING 
    No credentials provided.
static int	RESULT_STALE 
    Stale credentials provided.
static int	RESULT_UNKNOWN 
    Unknown user.
static int	RESULT_UNSUPPORTED 
    Unsupported credentials.
static int	RESULT_VALID 
    Valid credentials provided
	 */

	final static Logger logger = Logger.getLogger(CustomVerifier.class);

	@Override
	public int verify(Request request, Response response) {
		logger.trace("Called the override custom verify restlet method...");
		
		int result = RESULT_VALID;

		if (request.getChallengeResponse() == null) {
			result = RESULT_MISSING;
		} else {
			String identifier = getIdentifier(request, response);
			char[] secret = getSecret(request, response);
			result = verify(identifier, secret);
			
			String method= request.getMethod().toString();
			String url=request.getOriginalRef().toString().replace(request.getHostRef().toString().toLowerCase(), "");
			url = URLDecoder.decode(url);
		
//			String[] rootrefSplitted =request.getRootRef().toString().split("\\/");
//			String rootref= rootrefSplitted[rootrefSplitted.length-1];
//			String[] resourcerefSplitted=request.getResourceRef().toString().split(rootref);
//			String resource=resourcerefSplitted[resourcerefSplitted.length-1];
//			String url="/"+rootref+resource;
		
			JwtConsumer jwtConsumerSkipSign = new JwtConsumerBuilder()
	        .setDisableRequireSignature()
	        .setSkipSignatureVerification()
	        .build();
			String token= new String(secret);
			try {
				JwtClaims jwtExternalClaims = jwtConsumerSkipSign.processToClaims(token);
				JwtClaims jwtInternalClaims = jwtConsumerSkipSign.processToClaims(jwtExternalClaims.getClaimValue("token").toString());
				JSONObject	jsonClaim = new JSONObject(jwtInternalClaims.toJson());
				JSONObject action= jsonClaim.optJSONObject("action");
				String myMethod= action.optString("method");
				String myURL= "/"+action.optString("service")+"/"+ action.optString("url");
				logger.debug("myURL: "+ myURL );
				logger.debug("url: " + url);
				logger.debug("myMethod: " + myMethod);
				logger.debug("method: " + method);
				if(!myURL.equals(url) || !myMethod.equals(method)){
					result= RESULT_INVALID;
					logger.debug("Request mismatch token");
				}				
			} catch (InvalidJwtException e) {
				logger.error("InvalidJwtException", e);
				result= RESULT_INVALID;
			} catch (JSONException e) {
				logger.error("JSONException", e);
				result= RESULT_INVALID;
			}
			
			
			if (result == RESULT_VALID) {
				request.getClientInfo().setUser(createUser(identifier, request, response));
			}else{
				logger.debug("Credential validation failed!");
				response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			}
		}

		return result;
	}


	@Override
	public int verify(String identifier, char[] secret) {

		logger.trace("Called the custom verify restlet method");
		
		String token= new String(secret);
		JwtConsumer jwtConsumerSkipSign = new JwtConsumerBuilder()
        .setDisableRequireSignature()
        .setSkipSignatureVerification()
        .build();
		JwtClaims jwtExternalClaims;		
		try {
			Base64 b64coder = new Base64(true); 
			String keyService= PropertiesHelper.getProps().getProperty(Constants.HMAC_KEY);
			keyService= b64coder.encodeBase64URLSafeString(keyService.getBytes());
			jwtExternalClaims = jwtConsumerSkipSign.processToClaims(token);
			JwtClaims jwtInternalClaims = jwtConsumerSkipSign.processToClaims(jwtExternalClaims.getClaimValue("token").toString());
			String secureCode=(String) jwtInternalClaims.getClaimValue("secureService");
			String decryptSecure=AESencrpPS.decrypt(secureCode, keyService);
			
			JwtConsumer jwtConsumerCheckKeyBrow = new JwtConsumerBuilder()
		       .setVerificationKey(new HmacKey(decryptSecure.getBytes()))
		       .setRelaxVerificationKeyValidation()
		       .build();
			jwtExternalClaims = jwtConsumerCheckKeyBrow.processToClaims(token);
			
			logger.debug("external Sign Verified");
			
			JwtConsumer jwtConsumerCheckKeyService = new JwtConsumerBuilder()
		       .setVerificationKey(new HmacKey(keyService.getBytes()))
		       .setRelaxVerificationKeyValidation()
		       .build();
			jwtInternalClaims = jwtConsumerCheckKeyService.processToClaims(jwtExternalClaims.getClaimValue("token").toString());
			
			logger.debug("internal Sign Verified");
			return RESULT_VALID;
		} catch (InvalidJwtException e) {
			logger.error("InvalidJwtException", e);
			return RESULT_INVALID;
		}
		
		
	}

}

