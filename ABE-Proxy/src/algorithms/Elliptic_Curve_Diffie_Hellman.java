package algorithms;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.KeyAgreement;

import messages.EPK_Info;

/*
 * @author Salvador Pérez
 * @version 11/11/2016
 */

public class Elliptic_Curve_Diffie_Hellman {
	private String key_type;
	private String cryptographic_curve;
	private KeyPair kp;
	private byte[] shared_secret;
	private Concat_Key_Derivation_Function ckdf;
	private byte[] shared_sym_key;

	public Elliptic_Curve_Diffie_Hellman(String key_type, String cryptographic_curve){
		try{
			this.key_type = key_type;
			this.cryptographic_curve = cryptographic_curve;
			String curve_parameters = this.getCurveParameters(cryptographic_curve);
			ECGenParameterSpec gps = new ECGenParameterSpec(curve_parameters);
		    KeyPairGenerator kpg = KeyPairGenerator.getInstance(key_type);
		    kpg.initialize(gps);
		    this.kp = kpg.generateKeyPair();
		    kp.getPublic();
		    this.ckdf = new Concat_Key_Derivation_Function();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void ECDH_ES(EPK_Info epk_info) {
		try{
			PublicKey public_key = this.getForeignPublicKey(epk_info);		
			KeyAgreement ka = KeyAgreement.getInstance("ECDH");
			ka.init(this.kp.getPrivate());
			ka.doPhase(public_key, true);
			this.shared_secret = ka.generateSecret();
			System.out.println("SHARED SECRET: " + Base64.getEncoder().withoutPadding().encodeToString(this.shared_secret));
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void Concat_KDF(EPK_Info epk_info){
		try{
			ByteArrayOutputStream baos;
//			System.out.println("------");
			byte[] z = this.shared_secret;
//			for(byte b:z)
//				System.out.print(b + " ");
//			System.out.println("\n------");
			
			int keyDataLen = this.getKeyLength(epk_info.getEnc());
//			System.out.println(keyDataLen);
//			System.out.println("------");
			
			byte[] algorithmID_length = this.ckdf.intToFourBytes(epk_info.getEnc().length());
			byte[] algorithmID_name = epk_info.getEnc().getBytes();
			baos = new ByteArrayOutputStream();
	        baos.write(algorithmID_length);
	        baos.write(algorithmID_name);
	        byte[] algorithmID = baos.toByteArray();
//			for(byte b:algorithmID)
//				System.out.print(b + " ");
//			System.out.println("\n------");
			
			byte[] partyUInfo_length = this.ckdf.intToFourBytes(epk_info.getApu().length());
			byte[] partyUInfo_name = epk_info.getApu().getBytes();
			baos = new ByteArrayOutputStream();
			baos.write(partyUInfo_length);
	        baos.write(partyUInfo_name);
			byte[] partyUInfo = baos.toByteArray();
//			for(byte b:partyUInfo)
//				System.out.print(b + " ");
//			System.out.println("\n------");
			
			byte[] partyVInfo_length = this.ckdf.intToFourBytes(epk_info.getApv().length());
			byte[] partyVInfo_name = epk_info.getApv().getBytes();
			baos = new ByteArrayOutputStream();
			baos.write(partyVInfo_length);
	        baos.write(partyVInfo_name);
			byte[] partyVInfo = baos.toByteArray();
//			for(byte b:partyVInfo)
//				System.out.print(b + " ");
//			System.out.println("\n------");
			
			byte[] suppPubInfo = this.ckdf.intToFourBytes(keyDataLen);
//			for(byte b:suppPubInfo)
//				System.out.print(b + " ");
//			System.out.println("\n------");
			
			this.shared_sym_key = this.ckdf.concatKDF(z, keyDataLen, algorithmID, partyUInfo, partyVInfo, suppPubInfo, null);
//			System.out.println("SHARED SYMMETRIC KEY: " + Base64.getEncoder().withoutPadding().encodeToString(this.shared_sym_key));
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void Delete_ekeys() {
		this.kp = null;
	}
	
	private String getCurveParameters(String cryptographic_curve) throws Exception{
		switch (cryptographic_curve) {
		case "P-256":
			return "secp256r1";
		default:
			throw new Exception("This elliptic curve is not used");
		}
	}
	
	private int getKeyLength(String enc) throws Exception{
		switch (enc) {
		case "A128GCM":
			return 128;
		default:
			throw new Exception("This key length is not used");
		}
	}
	
	private PublicKey getForeignPublicKey(EPK_Info epk_info){
		PublicKey public_key = null;
		try{
			byte[] x_byte = Base64.getUrlDecoder().decode(epk_info.getJwk().getX());
			byte[] y_byte = Base64.getUrlDecoder().decode(epk_info.getJwk().getY());
			ECPoint pubPoint = new ECPoint(new BigInteger(x_byte),new BigInteger(y_byte));
			AlgorithmParameters parameters = AlgorithmParameters.getInstance(this.key_type);
			parameters.init(new ECGenParameterSpec(this.getCurveParameters(this.cryptographic_curve)));
			ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
			ECPublicKeySpec pubSpec = new ECPublicKeySpec(pubPoint, ecParameters);
			KeyFactory kf = KeyFactory.getInstance(this.key_type);
			public_key = kf.generatePublic(pubSpec);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return public_key;
	}
	
	public String getKey_type() {
		return key_type;
	}

	public String getCryptographic_curve() {
		return cryptographic_curve;
	}

	public KeyPair getKp() {
		return kp;
	}
	
	public String getXparameterB64(){
		ECPublicKey pub_key  = (ECPublicKey)this.kp.getPublic();
		return Base64.getEncoder().withoutPadding().encodeToString(pub_key.getW().getAffineX().toByteArray());
	}

	public String getYparameterB64(){
		ECPublicKey pub_key  = (ECPublicKey)this.kp.getPublic();
		return Base64.getEncoder().withoutPadding().encodeToString(pub_key.getW().getAffineY().toByteArray());
	}
	
	public String getXparameterB64url(){
		System.out.println("****_____X______****");
		ECPublicKey pub_key  = (ECPublicKey)this.kp.getPublic();
//		System.out.println("num:"+pub_key.getW().getAffineX().toString());
//		System.out.println("number b64url: "+Base64.getUrlEncoder().withoutPadding().encodeToString(pub_key.getW().getAffineX().toByteArray()));
//		System.out.println("lenght:"+pub_key.getW().getAffineX().toByteArray().length);
		
		BigInteger x= pub_key.getW().getAffineX();
		byte[] array = x.toByteArray();
		if (array[0] == 0) {
		    byte[] tmp = new byte[array.length - 1];
		    System.arraycopy(array, 1, tmp, 0, tmp.length);
		    array = tmp;
		}
//		System.out.println(" new lenght:"+array.length);
//		System.out.println("new:"+new BigInteger(array));
//		System.out.println("new b64url: "+Base64.getUrlEncoder().withoutPadding().encodeToString(array));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(array);
	}

	public String getYparameterB64url(){
		System.out.println("****_____Y______****");
		ECPublicKey pub_key  = (ECPublicKey)this.kp.getPublic();
//		System.out.println("num:"+pub_key.getW().getAffineX().toString());
//		System.out.println("number b64url: "+Base64.getUrlEncoder().withoutPadding().encodeToString(pub_key.getW().getAffineY().toByteArray()));
//		System.out.println("lenght:"+pub_key.getW().getAffineY().toByteArray().length);
		BigInteger y= pub_key.getW().getAffineY();
		byte[] array = y.toByteArray();
		if (array[0] == 0) {
		    byte[] tmp = new byte[array.length - 1];
		    System.arraycopy(array, 1, tmp, 0, tmp.length);
		    array = tmp;
		}
//		System.out.println(" new lenght:"+array.length);
//		System.out.println("new:"+new BigInteger(array));
//		System.out.println("new b64url: "+Base64.getUrlEncoder().withoutPadding().encodeToString(array));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(array);
	}
	
	public byte[] getShared_Sym_Key() {
		return shared_sym_key;
	}
	
}