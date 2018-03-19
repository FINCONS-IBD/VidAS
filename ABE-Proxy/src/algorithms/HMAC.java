package algorithms;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HMAC {

	public static byte[] generateHMACKeySha256(byte[] shared_sym_key) {
		MessageDigest md;
		byte[] key2=null;
		try {
			md = MessageDigest.getInstance("SHA-256");
	        key2 = md.digest(shared_sym_key);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return key2;
	}
	
	public static byte[] signHMACSHA256(byte[] data, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException{
		SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA256");
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(signingKey);
		return mac.doFinal(data);
	
	}
	
	
	
}
