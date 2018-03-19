package algorithms;

import java.security.AlgorithmParameters;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/*
 * @author Salvador Pérez
 * @version 11/11/2016
 *
 * 
 * @modifier Diego Pedone added iv
 * @version 25/05/2017
 */

public class AES {
	public byte[] AES_encrypt(byte[] cpabe_info, byte[] shared_sym_key){
		byte[] encVal = null;
		try{
			Key key = new SecretKeySpec(shared_sym_key, "AES");
	        Cipher c = Cipher.getInstance("AES");
	        c.init(Cipher.ENCRYPT_MODE, key);
//			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
//	        c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
			encVal = c.doFinal(cpabe_info);
		}catch(Exception e){
			e.printStackTrace();
		}
		return encVal;
	}

	public byte[] AES_decrypt(byte[] enc_cpabe_info, byte[] shared_sym_key){
		byte[] encVal = null;
		try{
			Key key = new SecretKeySpec(shared_sym_key, "AES");
//	        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
			Cipher c = Cipher.getInstance("AES");
	        c.init(Cipher.DECRYPT_MODE, key);
	        encVal = c.doFinal(enc_cpabe_info);
		}catch(Exception e){
			e.printStackTrace();
		}
		return encVal;
	}
	
	
	public byte[] AES_encrypt(byte[] cpabe_info, byte[] shared_sym_key, byte[] iv ){
		byte[] encVal = null;
		try{
			Key key = new SecretKeySpec(shared_sym_key, "AES");
			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
			encVal = c.doFinal(cpabe_info);
		}catch(Exception e){
			e.printStackTrace();
		}
		return encVal;
	}

	public byte[] AES_decrypt(byte[] enc_cpabe_info, byte[] shared_sym_key, byte[] iv){
		byte[] encVal = null;
		try{
			Key key = new SecretKeySpec(shared_sym_key, "AES");
	        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
	        encVal = c.doFinal(enc_cpabe_info);
		}catch(Exception e){
			e.printStackTrace();
		}
		return encVal;
	}
}