package com.fincons.restlet;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jose4j.json.JsonUtil;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.security.SecretVerifier;

//import com.fincons.token.utils.AESencrp;
import com.fincons.utils.Constants;
import com.fincons.utils.CryptUtil;
import com.fincons.utils.DateUtil;
import com.fincons.utils.PropertiesHelper;

public class CustomVerifier extends SecretVerifier {

	/*
	 * static int RESULT_INVALID Invalid credentials provided. static int
	 * RESULT_MISSING No credentials provided. static int RESULT_STALE Stale
	 * credentials provided. static int RESULT_UNKNOWN Unknown user. static int
	 * RESULT_UNSUPPORTED Unsupported credentials. static int RESULT_VALID Valid
	 * credentials provided
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

			if (result == RESULT_VALID) {
				request.getClientInfo().setUser(createUser(identifier, request, response));
			} else {
				logger.debug("Credential validation failed!");
				Map<String, Object> attributes = new HashMap<String, Object>();
				attributes.put("auth_failed", "true");
				response.setAttributes(attributes);
				response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
			}
		}

		return result;
	}

	@Override
	public int verify(String identifier, char[] secret) {

		logger.trace("Called the custom verify restlet method");
		logger.info("The user try to perform a login action");

		String JSONResponseToken = new String(secret);

		logger.debug("### Start Generate External Token ###");
		JwtClaims claims = new JwtClaims();
		claims.setIssuedAtToNow();
		claims.setExpirationTimeMinutesInTheFuture(2); // time when the token will expire (1 minutes from now)
		claims.setSubject("Proxy Video Service");
		try {
			claims.setClaim("token", JSONResponseToken);

			logger.info("Claims generation succesfull completed!");

			JsonWebSignature jws = new JsonWebSignature();
			jws.setPayload(claims.toJson());
			jws.setKey(new HmacKey((PropertiesHelper.getProps().getProperty(Constants.HMAC_KEY)).getBytes()));
			jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
			logger.debug("### End Generate External Token ###");

			String jwt = jws.getCompactSerialization();
			logger.debug("Token created!");

			ClientResource clientResource = new ClientResource(
					PropertiesHelper.getProps().getProperty("proxyLoginServicePath"));
			Representation serverResponse = clientResource.post(jwt);

			logger.debug("The POST operation on the service "
					+ PropertiesHelper.getProps().getProperty("proxyLoginServicePath")
					+ " was sucesfully completed...");
			
			String responseTokenService = serverResponse.getText();
			JSONObject responseTS = new JSONObject(responseTokenService);
			JSONObject contentResponseTS = responseTS.optJSONObject("response");

			logger.debug("The Access Token Service response with the following result: " + contentResponseTS);

			if (responseTS != null && contentResponseTS.optInt("code") == 200) {
				logger.debug("The Access Token Service success response is valid...");
				String responseHMAC = responseTS.optString("HMAC");
				String hmac = CryptUtil.getHMAC(PropertiesHelper.getProps().getProperty(Constants.HMAC_KEY),
						contentResponseTS.toString());
				if (responseHMAC.equals(hmac)) {
					logger.info("HMAC validation passed! " + identifier + " AUTHENTICATED!");
					return RESULT_VALID;
				} else {
					logger.info("HMAC validation failed! AUTHENTICATION FAILED for the user " + identifier);
					logger.debug("responseHMAC:" + responseHMAC);
					logger.debug("contentResponseTS:" + contentResponseTS);
					logger.debug("responseTS:" + responseTS);
					logger.debug("hmac:" + hmac);
					return RESULT_INVALID;
				}
			} else {
				logger.info("The Access Token Service response contains an error code or is empty for the user "
						+ identifier);
				return RESULT_INVALID;
			}
		} catch (ResourceException e) {
			logger.error("ResourceException", e);
			e.printStackTrace();
			return RESULT_INVALID;
		} catch (JoseException e) {
			logger.error("Internal Server Error, Error in JWT Generation", e);
			return RESULT_INVALID;
		} catch (JSONException e) {
			logger.error("JSONException", e);
			e.printStackTrace();
			return RESULT_INVALID;
		} catch (IOException e) {
			logger.error("IOException", e);
			e.printStackTrace();
			return RESULT_INVALID;
		} catch (InvalidKeyException e) {
			logger.error("InvalidKeyException", e);
			e.printStackTrace();
			return RESULT_INVALID;
		} catch (NoSuchAlgorithmException e) {
			logger.error("NoSuchAlgorithmException", e);
			e.printStackTrace();
			return RESULT_INVALID;
		}
	}
}
