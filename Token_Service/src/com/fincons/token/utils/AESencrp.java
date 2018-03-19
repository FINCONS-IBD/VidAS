package com.fincons.token.utils;

import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

/**
 * An util AES Encryption/Decryption Class with secret server side symmetric key
 * @author leonardo.straniero
 *
 */
public class AESencrp {

	final static Logger logger = Logger.getLogger(AESencrp.class);
	
	public static String encrypt(byte[] decryptedData, String keyValue) {

		logger.info("Called the encrypt method..." +keyValue);

		
		String encryptedValue = null;
		org.apache.tomcat.util.codec.binary.Base64 b64enc= new org.apache.tomcat.util.codec.binary.Base64(true);
		byte[] myKey = b64enc.decodeBase64(keyValue);
	
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(myKey); // Change this to "UTF-16" if needed
			byte[] digest = md.digest();
			String digestring="";
			for (byte b : digest) {
				digestring+=b+",";
			}

			SecretKeySpec secret_key = new SecretKeySpec(digest, "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] iv=Arrays.copyOfRange(digest, 0, 16);
			logger.trace("BA iv:" +Arrays.toString(iv) );
			cipher.init(Cipher.ENCRYPT_MODE, secret_key, new IvParameterSpec(iv));
	
			byte[] encVal = cipher.doFinal(decryptedData);
			logger.trace("encVal" + Arrays.toString(encVal));
			encryptedValue = b64enc.encodeBase64URLSafeString(encVal);
			logger.trace("encVal" + encryptedValue);
		} catch (Exception e) {
			logger.error("AES Encryption problems!", e);
			return null;
		}
		return encryptedValue;
	}
}