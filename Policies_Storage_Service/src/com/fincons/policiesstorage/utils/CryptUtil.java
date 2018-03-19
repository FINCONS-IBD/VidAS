package com.fincons.policiesstorage.utils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.codec.binary.Base64;

public class CryptUtil {
	
	final static Logger logger = Logger.getLogger(CryptUtil.class);
	
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
