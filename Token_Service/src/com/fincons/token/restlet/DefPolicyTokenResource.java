package com.fincons.token.restlet;

import java.security.SecureRandom;
import java.util.Date;

import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.codec.binary.Base64;
import org.jose4j.json.JsonUtil;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import com.fincons.h2.H2DatabaseOperation;
import com.fincons.h2.UserForH2;
import com.fincons.token.restlet.ldap.LdapManager;
import com.fincons.token.utils.AESencrp;
import com.fincons.token.utils.PropertiesHelper;

public class DefPolicyTokenResource extends ServerResource {
	final static Logger logger = Logger.getLogger(DefPolicyTokenResource.class);
	private static Base64 b64coder = new Base64(true);

	@Get
	public Representation generateToken(String parameters) {
		logger.trace("Called the generateToken() method...");

		logger.debug("### Start generateToken ###");
		Series<Header> headers = (Series<Header>) getRequestAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
		String user_token = headers.getFirstValue("authorization");

		JSONObject jsonResponse = new JSONObject();
		JwtConsumer jwtConsumerSkipSign = new JwtConsumerBuilder().setDisableRequireSignature()
				.setSkipSignatureVerification().build();
		JwtClaims jwtClaims;
		String userSecret = "";
		String username = "";
		JSONObject userLdap = null;
		String serviceRequest = "";
		try {
			// VALIDAZIONE TOKEN
			logger.debug("### Start Token Validation ###");
			try {
				jwtClaims = jwtConsumerSkipSign.processToClaims(user_token);
				username = jwtClaims.getSubject();
				UserForH2 user = H2DatabaseOperation.selectUserByUsername(username);
				if (user != null) {
					if (user.getValidityTime().before(new Date())) {
						logger.info(
								"User Validity time expired. The service return a status 403 with \"Session Timeout\"");
						getResponse().setStatus(new Status(403));
						return new JsonRepresentation(getJsonError(403, "Session Timeout"));
					} else {
						logger.info("User Validity is ok");
						userSecret = user.getUserSecret();
					}
				} else {
					logger.info(
							"User not present in H2 DB. Request rejected! The service return a status 404 with \"User Not Found\"");
					getResponse().setStatus(new Status(404));
					return new JsonRepresentation(getJsonError(404, "User Not Found"));
				}
			} catch (InvalidJwtException e) {
				logger.error("Invalid JWT, Request Not Valid ", e);
				getResponse().setStatus(new Status(403));
				return new JsonRepresentation(getJsonError(403, "Token Timeout"));
			} catch (MalformedClaimException e) {
				logger.error("Malformed Claim, Request Not Valid", e);
				getResponse().setStatus(new Status(500));
				return new JsonRepresentation(
						getJsonError(500, "Malformed Claim, Request Not Valid : " + e.getMessage()));
			}

			JSONObject jsonClaim = null;
			try {
				logger.debug("Check User Secret...");
				JwtConsumer jwtConsumer = new JwtConsumerBuilder()
						.setVerificationKey(new HmacKey(userSecret.getBytes())).setRelaxVerificationKeyValidation()
						.build();
				jwtClaims = jwtConsumer.processToClaims(user_token);
				jsonClaim = new JSONObject(jwtClaims.toJson());
			} catch (InvalidJwtException e) {
				logger.error("Invalid userSecret", e);
				getResponse().setStatus(new Status(401));
				return new JsonRepresentation(getJsonError(401, "UserSecret mismatch"));
			} catch (JSONException e) {
				logger.fatal("Internal Server Error, Error in JSON Parser ", e);
				getResponse().setStatus(new Status(500));
				return new JsonRepresentation(getJsonError(500, "Error in JSON Parser : " + e.getMessage()));
			}
			logger.debug("### End Token Validation ###");

			try {
				userLdap = LdapManager.searchUser(new JSONObject().put("mail", username));
			} catch (NamingException ne) {
				logger.error("Error in LDAP", ne);
				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", 500);
				jsonObjectRet.put("message", "LDAP Service not available or timeout received");
				logger.info("LDAP Service not available or timeout received");
				return new JsonRepresentation(jsonObjectRet);
			}
			JSONObject actionRequest = jsonClaim.optJSONObject("action");

			serviceRequest = actionRequest.optString("service");
			logger.debug("### Start Token Authorization ### " + userLdap.toString());
		} catch (JSONException e) {
			getResponse().setStatus(new Status(406));
			return new JsonRepresentation(getJsonError(406, "Error in control rules"));
		}

		logger.debug("### End Token Authorization ###");

		String defaultPolicy = LdapManager.getDefaultPolicy(username);
		if (defaultPolicy != null && !defaultPolicy.equals("")) {
			JSONObject action = null;

			String sharedServiceKey = PropertiesHelper.getProps().getProperty(serviceRequest + "SharedKey");

			logger.debug("Get Shered Key for" + serviceRequest);

			sharedServiceKey = b64coder.encodeBase64URLSafeString(sharedServiceKey.getBytes());

			// Genero il nuovo token
			logger.debug("### Start Generate Token ###");
			JwtClaims claims = new JwtClaims();
			claims.setExpirationTimeMinutesInTheFuture(1); // time when the
															// token will expire
															// (1 minutes from
															// now)
			claims.setSubject("AccessTokenService");
			try {
				action = new JSONObject();
				action.put("method", "GET");
				action.put("service", serviceRequest);
				action.put("url", defaultPolicy);
				claims.setClaim("action", JsonUtil.parseJson(action.toString()));
			} catch (JSONException je) {
				logger.error("Internal Server Error, Error Parsing Action in JSON", je);
				getResponse().setStatus(new Status(500));
				return new JsonRepresentation(getJsonError(500, "Error in Token Generation : " + je.getMessage()));
			} catch (JoseException je) {
				logger.error("Internal Server Error, Error Parsing Action in JSON", je);
				getResponse().setStatus(new Status(500));
				return new JsonRepresentation(getJsonError(500, "Error in Token Generation : " + je.getMessage()));
			}

			SecureRandom secureRandom = new SecureRandom();

			byte[] secure = secureRandom.generateSeed(32);

			claims.setClaim("secureBrow", AESencrp.encrypt(secure, userSecret));
			claims.setClaim("secureService", AESencrp.encrypt(secure, sharedServiceKey));

			logger.info("Claims generation succesfull completed!");

			JsonWebSignature jws = new JsonWebSignature();
			jws.setPayload(claims.toJson());
			jws.setKey(new HmacKey(sharedServiceKey.getBytes()));
			jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
			try {
				String jwt = jws.getCompactSerialization();

				jsonResponse.put("code", 200);
				jsonResponse.put("message", "Authorizated Token Generated");
				jsonResponse.put("jwt", jwt);
				logger.info(
						"JWT generation successful completed. The service return status 200 and message \"Authorizated Token Generated\"");
				logger.debug("### End Generate Token ###");
			} catch (JoseException e) {
				logger.error("Internal Server Error, Error in JWT Generation", e);
				getResponse().setStatus(new Status(500));
				return new JsonRepresentation(getJsonError(500, "Error in Token Generation : " + e.getMessage()));
			} catch (JSONException e) {
				logger.error("Internal Server Error, Error in JSON Parser ", e);
				getResponse().setStatus(new Status(500));
				return new JsonRepresentation(getJsonError(500, "Error in JSON Parser : " + e.getMessage()));
			}
			getResponse().setStatus(new Status(200));
			return new JsonRepresentation(jsonResponse);
		} else{
			logger.error("Error, Default policy not found");
			getResponse().setStatus(new Status(404));
			return new JsonRepresentation(getJsonError(404, "Default policy not found"));
		}
	}

	public static JSONObject getJsonError(int code, String message) {
		JSONObject errorJson = new JSONObject();
		try {
			errorJson.put("code", code);
			errorJson.put("message", message);
		} catch (JSONException e) {
			logger.error("Error in create ErrorJson code:" + code + "message:'" + message + "'.", e);
		} finally {
			return errorJson;
		}
	}
}
