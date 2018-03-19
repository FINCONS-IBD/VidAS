package com.fincons.policiesstorage.utils;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.codec.binary.Base64;

/**
 * An util AES Encryption/Decryption Class with secret server side symmetric key
 * @author leonardo.straniero
 *
 */
public class AESencrpPS {

	final static Logger logger = Logger.getLogger(AESencrpPS.class);
	
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
			System.out.println(digestring);
			SecretKeySpec secret_key = new SecretKeySpec(digest, "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] iv=Arrays.copyOfRange(digest, 0, 16);
		
			cipher.init(Cipher.ENCRYPT_MODE, secret_key, new IvParameterSpec(iv));
	
			byte[] encVal = cipher.doFinal(decryptedData);
			encryptedValue = b64enc.encodeBase64URLSafeString(encVal);
			
		} catch (Exception e) {
			logger.error("AES Encryption problems!", e);
			return null;
		}
		return encryptedValue;
	}
	
	public static String decrypt(String encryptData, String keyValue) {

		logger.info("Called the encrypt method..." +keyValue);

		
		String decryptedValue = null;
		org.apache.tomcat.util.codec.binary.Base64 b64enc= new org.apache.tomcat.util.codec.binary.Base64(true);
		byte[] myKey = b64enc.decodeBase64(keyValue);
		byte[] encDat = b64enc.decodeBase64(encryptData);
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
		
			cipher.init(Cipher.DECRYPT_MODE, secret_key, new IvParameterSpec(iv));
	
			byte[] decVal = cipher.doFinal(encDat);
			decryptedValue = b64enc.encodeBase64URLSafeString(decVal);
			
		} catch (Exception e) {
			logger.error("AES Decryption problems!", e);
			return null;
		}
		return decryptedValue;
	}
	
}