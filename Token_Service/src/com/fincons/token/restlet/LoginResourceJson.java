package com.fincons.token.restlet;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.fincons.h2.H2DatabaseOperation;
import com.fincons.h2.UserForH2;
import com.fincons.token.restlet.ldap.LdapManager;
import com.fincons.token.utils.Constants;
import com.fincons.token.utils.CryptUtil;
import com.fincons.token.utils.DateUtil;
import com.fincons.token.utils.PropertiesHelper;

public class LoginResourceJson extends ServerResource {
	final static Logger logger = Logger.getLogger(LoginResourceJson.class);

	@Post
	public Representation login(String parameters) {
		logger.trace("Called the login() method...");

		int status = 0;
		String message = "";
		JSONObject jsonResponse = new JSONObject();
		JSONObject jsonObject = new JSONObject();

		try {
			JwtConsumer jwtConsumerSkipSign = new JwtConsumerBuilder().setDisableRequireSignature()
					.setSkipSignatureVerification().build();
			JSONObject request = null;
			JwtClaims jwtExternalClaims = null;
			JwtClaims jwtInternalClaims = null;
			String internalToken=null;
			boolean flagTokenNested=false;
			try {
				jwtExternalClaims = jwtConsumerSkipSign.processToClaims(parameters);
				if(jwtExternalClaims.hasClaim("token")){
					flagTokenNested=true;
					internalToken=jwtExternalClaims.getClaimValue("token").toString();
					jwtInternalClaims = jwtConsumerSkipSign.processToClaims(internalToken);
				}else{
					internalToken=parameters;
					jwtInternalClaims=jwtExternalClaims;
				}
				JSONObject jsonClaim = new JSONObject(jwtInternalClaims.toJson());
				request = jsonClaim.optJSONObject("request");
			} catch (InvalidJwtException e) {
				logger.error("InvalidJwtException", e);
			} catch (JSONException e) {
				logger.error("JSONException", e);
			}
			JSONObject data = request.optJSONObject("data");
			String username = data.optString("username");
			long challengeIDReq = data.optLong("challengeID");
		
			// Entro nel payload(o claims) del token esterno.
			// Entro nel payload(o claims) del token interno e recupero lo
			// username.
			// Eseguo questo controllo. START
			logger.debug("Check username in H2 DB: " + username);
			if (username.isEmpty() || !H2DatabaseOperation.checkUser(username)) {
				logger.info(
						"Json not valid! Empty username. The service return status 500 and message \"Internal Server Error\"");
				status = 500;
				message = "Internal Server Error";
				getResponse().setStatus(new Status(status));
				jsonResponse.put("code", status);
				jsonResponse.put("message", message);
				return new JsonRepresentation(jsonResponse);
			}

			// Recupero l'utente dal database
			logger.debug("Retrieve the username from H2 DB: " + username);
			UserForH2 user = H2DatabaseOperation.selectUserByUsername(username);
			long challengID = user.getChallengeID();
			if(challengID != challengeIDReq){
				logger.error("Unauthorized");
				getResponse().setStatus(new Status(401));
				return new JsonRepresentation(jsonResponse);
			}
			Date timestamp = user.getTimestamp();
			String userSecret = user.getUserSecret();
			// Eseguo questo controllo. END

			// Con lo user secret verifico la firma del token interno
			boolean verified = false;
			try {
				JwtConsumer jwtConsumerCheckKeyInternal = new JwtConsumerBuilder()
						.setVerificationKey(new HmacKey(userSecret.getBytes())).setRelaxVerificationKeyValidation()
						.build();
				jwtExternalClaims = jwtConsumerCheckKeyInternal
						.processToClaims(internalToken);
				//jwtExternalClaims.getClaimValue("token").toString());
				logger.debug("internal Sign Verified");
				
				if(flagTokenNested){
					JwtConsumer jwtConsumerCheckKeyExternal = new JwtConsumerBuilder()
							.setVerificationKey(
									new HmacKey((PropertiesHelper.getProps().getProperty(Constants.HMAC_KEY)).getBytes()))
							.setRelaxVerificationKeyValidation().build();
					jwtExternalClaims = jwtConsumerCheckKeyExternal.processToClaims(parameters);
				}	
				
				logger.debug("external Sign Verified");
				verified = true;

			} catch (InvalidJwtException invalid) {
				logger.error("InvalidJwtException", invalid);
			}

			// Con lo HMAK_KEY verifico la firma del token Esterno
			// Cambio la condizione seguente...
			// ADESSO Se le 2 verifiche (token interno, esterno) vanno a buon
			// fine eseguo l'interno dell'if altrimenti l'else
			if (verified) {
				logger.info(
						"Token signatures comparison sucessful completed. The service return status 200 and message \"Login sucessful completed\"");
				status = 200;
				message = "Login sucessful completed";
				jsonObject.put("code", status);
				jsonObject.put("message", message);
				jsonObject.put("timestamp", DateUtil.GetUTCdatetimeAsString());
				if(flagTokenNested){
					String HMACResponse = CryptUtil.getHMAC(PropertiesHelper.getProps().getProperty(Constants.HMAC_KEY),
							jsonObject.toString());
					jsonResponse.put("HMAC", HMACResponse);
				}
				jsonResponse.put("response", jsonObject);
				
				JSONObject newCertificate = data.optJSONObject("certificate");
				try {
					boolean registered = LdapManager.setCertificate(username, newCertificate);
					if(!registered){
						logger.error("Unauthorized ");
						getResponse().setStatus(new Status(401));
						return new JsonRepresentation(jsonResponse);
					}
				} catch (AuthenticationNotSupportedException e) {
					logger.error("Authentication not supported ", e);
					e.printStackTrace();
					getResponse().setStatus(new Status(500));
					return new JsonRepresentation(jsonResponse);
					
				} catch (AuthenticationException e) {
					logger.error("Authentication error ", e);
					e.printStackTrace();
					getResponse().setStatus(new Status(500));
					return new JsonRepresentation(jsonResponse);
				} catch (NamingException e) {
					logger.error("Naming error ", e);
					e.printStackTrace();
					getResponse().setStatus(new Status(500));
					return new JsonRepresentation(jsonResponse);
				}
				
			} else {
				logger.info("Token signatures comparison failed! The service return status 401 and message \"Login Failed\"");
				status = 401;
				message = "Login Failed";
				// Login Fallito
				jsonResponse.put("code", status);
				jsonResponse.put("message", message);
			}

		} catch (JSONException e) {
			logger.error("Internal server error", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonResponse);
		} catch (InvalidKeyException e) {
			logger.error("Internal server error", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonResponse);
		} catch (NoSuchAlgorithmException e) {
			logger.error("Internal server error", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonResponse);
		}
		getResponse().setStatus(new Status(status));
		return new JsonRepresentation(jsonResponse);
	}

}
