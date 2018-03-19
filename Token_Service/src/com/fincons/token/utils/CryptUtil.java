package com.fincons.token.utils;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.util.BigIntegers;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;
import org.json.JSONException;
import org.json.JSONObject;

public class CryptUtil {
	
	final static Logger logger = Logger.getLogger(CryptUtil.class);
	
	
	/**
	 * 
	 * @param accessTokenPublicKey
	 * @return JSONObject userData con actkSrvPublicKey, validityTime, challengeID
	 * @throws JSONException
	 * @throws JoseException
	 * 
	 */
	public static JSONObject generateUserData(PublicKey accessTokenPublicKey) throws JSONException, JoseException{
		logger.trace("Called the generateUserData method...");

		PublicJsonWebKey jwkp = PublicJsonWebKey.Factory.newPublicJwk(accessTokenPublicKey);
		String publicKey=jwkp.toJson();
		DateFormat fomratDate= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		fomratDate.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date now = new Date();
		long nowMillisecond=now.getTime();
		String validityInHours= PropertiesHelper.getProps().getProperty(Constants.VALIDITY);
		long validity=Integer.parseInt(validityInHours) * 60 *60 * 1000 ;//in millisecond
		
		Date deadline= new Date(nowMillisecond+validity);
		SecureRandom secureRandom= new SecureRandom();		
		long challengeID=secureRandom.nextInt(999999999);
		
		JSONObject userData= new JSONObject();
		userData.put("actkSrvPublicKey", new JSONObject(publicKey));
		userData.put("validityTime", DateUtil.DateToStringDate(deadline));
		userData.put("challengeID", challengeID);
		
		return userData;
		
	}

	/**
	 * Genera lo UserSecret e lo restituisce in forma byteArray
	 * @param publicKeyB
	 * @param privateKeyA
	 * @return
	 */
	public static byte[] calculateECDHBasicAgreement(ECPublicKeyParameters publicKeyB,ECPrivateKeyParameters privateKeyA){
		logger.trace("Called the calculateECDHBasicAgreement method...");

		ECDHBasicAgreement basicAgreement = new ECDHBasicAgreement();
		basicAgreement.init(privateKeyA);
		BigInteger userSecret = basicAgreement.calculateAgreement(publicKeyB);
		return BigIntegers.asUnsignedByteArray(userSecret);
		
	}


	
	public static String getHMAC(String key, String stringa) throws InvalidKeyException, NoSuchAlgorithmException{
		logger.trace("Called the getHMAC method...");

	   Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
	   SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
	   sha256_HMAC.init(secret_key);
	
	   String hash = Base64.encodeBase64String(sha256_HMAC.doFinal(stringa.getBytes()));
	   
	   logger.info("HMAC generation successful completed...");
	   return hash;
	}
		 
	
}
