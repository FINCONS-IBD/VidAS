package com.fincons.token.restlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.util.Date;

import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.swing.text.NavigationFilter;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.codec.binary.Base64;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fincons.h2.H2DatabaseOperation;
import com.fincons.h2.UserForH2;
import com.fincons.token.restlet.ldap.LdapManager;
import com.fincons.token.utils.Constants;
import com.fincons.token.utils.CryptUtil;
import com.fincons.token.utils.DateUtil;
import com.fincons.token.utils.PropertiesHelper;

public class UserDataResourceImpl extends ServerResource {

	final static Logger logger = Logger.getLogger(UserDataResourceImpl.class);
	final static Base64 b64coder = new Base64(true);

	@Post
	public Representation getUserSecret(String parameters){
		logger.trace("Called the createInboundRoot restlet method...");

		int status = 0;
		String message = "";
		JSONObject object = new JSONObject();
		JSONObject jsonError = new JSONObject();
		try {
			jsonError.put("code", 500);
			jsonError.put("message", "Internal Server Error");

			JSONObject obj = new JSONObject(parameters);
			String username=obj.optString("username");
			JSONObject loginUserData = new JSONObject();
			
			logger.info("Called the getUserData method with following parameters: " + username);

			if (username.isEmpty()) {
				
				status = 500;
				message = "Internal Server Error";
				getResponse().setStatus(new Status(status));

				logger.info("Json not valid! Empty username. The service return a code " +  status + " and a message " + message);
				
				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", status);
				jsonObjectRet.put("message", message);
				return new JsonRepresentation(jsonObjectRet);
			}

			logger.info("Calling Chek User on DB and LDAP...");
			JSONObject user =null;
			try{
				user = LdapManager.searchUser(new JSONObject().put("mail", username));
				status = user.has("code") ? user.getInt("code") : 500;
			} catch (NamingException ne) {
				logger.error("Error in LDAP", ne);
				status= 500;
				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", 500);
				jsonObjectRet.put("message", "LDAP Service not available or timeout received");
				logger.info("LDAP Service not available or timeout received");
				return new JsonRepresentation(jsonObjectRet);
			}

			if (status != 200 || user.opt("certificate").equals("null")) {
				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", 401);
				jsonObjectRet.put("message", "User Unauthorized");
				logger.info("User Unauthorized. The service return a code 401 and a message \"User Unauthorized\"");
				return new JsonRepresentation(jsonObjectRet);
			}
			
			loginUserData.put("userCertificate", user.opt("certificate"));
			object.put("code", status);
			// Se l'utente è presente nel database, cancello il vecchio e genero
			// nuove userSecret & ChallengeID
			logger.debug("Update the old user information...");
			if (H2DatabaseOperation.checkUser(username)) {
				H2DatabaseOperation.deleteUserFromUsername(username);
			}
		
			// ****** RECUPERO CHIAVE PUBBLICA *******
			logger.debug("Retrieve the public key from user certificate...");
			String keyFromDevice = user.optString("certificate");
			keyFromDevice = keyFromDevice.replaceAll("'", "\"");
			JSONObject keys = new JSONObject(keyFromDevice);
			String pubKeyFromDevice = keys.optString("publicKey");
			PublicJsonWebKey jwkp = PublicJsonWebKey.Factory.newPublicJwk(pubKeyFromDevice);
			ECPublicKey publicKey= (ECPublicKey)jwkp.getKey();
			logger.debug("Public key generate successful...");

			// ****** GENERAZIONE EFFIMERALKEY********
			logger.debug("Effimeral key generation...");
			ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
			KeyPairGenerator g = KeyPairGenerator.getInstance("ECDH", new BouncyCastleProvider());
			g.initialize(ecSpec, new SecureRandom());
			KeyPair aKeyPair = g.generateKeyPair();
			PrivateKey accesstokenPriKey = aKeyPair.getPrivate();
			PublicKey accesstokenPubKey = aKeyPair.getPublic();
			logger.debug("Effimeral Key generate successful...");

			// ****** GENERAZIONE USERSECRET ACCESSTOKEN********
			logger.debug("UserSecret generation...");
			ECPublicKeyParameters atpubKey = (ECPublicKeyParameters) PublicKeyFactory.createKey(publicKey.getEncoded());
			ECPrivateKeyParameters atprivKey = (ECPrivateKeyParameters) PrivateKeyFactory.createKey(accesstokenPriKey.getEncoded());
			byte[] userSecret = CryptUtil.calculateECDHBasicAgreement(atpubKey, atprivKey);
			String userSecretString=b64coder.encodeBase64URLSafeString(userSecret);
			JSONObject userData = CryptUtil.generateUserData(accesstokenPubKey);
			logger.debug("UserSecret generate successful...");
			
			// MEMORIZZO Username UserSecret(formato esadecimale) e UserData
			Date timestamp = DateUtil.GetUTCdatetimeAsDate();
			boolean esitoInsert=H2DatabaseOperation.insertUser(new UserForH2(username, userSecretString, userData, timestamp));
						
			if(esitoInsert){
				logger.info("UserSecret and UserData persisted into H2 DB...");
				
				loginUserData.put("userData", userData);
				loginUserData.put("timestamp", DateUtil.DateToStringDate(timestamp));
				object.put("loginUserData", loginUserData);
				//HMAC object
				String signature=CryptUtil.getHMAC(PropertiesHelper.getProps().getProperty(Constants.HMAC_KEY),loginUserData.toString());
				object.put("HMACloginUserData", signature);
				
				logger.debug("Json Response successful created...");
			}else{
				status = 500;
				message = "Internal Server Error";
				getResponse().setStatus(new Status(status));

				JSONObject jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", status);
				jsonObjectRet.put("message", message);
				
				logger.error("Error during insertion User information in H2 DB! The service return code " + status +" and message "+ message );
				
				return new JsonRepresentation(jsonObjectRet);
			}
		} catch (JSONException e) {
			logger.error("Internal server error", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonError);
		} catch (NoSuchAlgorithmException e) {
			logger.error("Internal server error", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonError);
		} catch (InvalidAlgorithmParameterException e) {
			logger.error("Internal server error", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonError);
		} catch (IOException e) {
			logger.error("Internal server error", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonError);
		} catch (JoseException e) {
			logger.error("Internal server error", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonError);
		} catch (InvalidKeyException e) {
			logger.error("Internal server error", e);
			e.printStackTrace();
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonError);
		}
		
		getResponse().setStatus(new Status(status));
		
		logger.info("getUserSecret service successful completed. The serivce return code " + status);
		
		return new JsonRepresentation(object);
	}
	
	
	public static void main(String[] args) throws UnsupportedEncodingException {
		String stringone="{\"userCertificate\": \"{'publicKey':'AkivZp+MkX5mxK6bm4K9cDb0YJeZe62KlmiA8gT4G/Rz',"
				+ "'privateKey':'3b2a816865093c2d375a1c34aef802cc33da70ce1e8a091ca405a7de479f257ca1c2d5a6bf5ca64cd6942c381336dde4'}\","
				+ "\"userData\": {\"validityTime\": \"2016-04-15T16:51:38\",\"challengeID\": 236745355,\"actkSrvPublicKey\": {\"kty\": \"EC\",\"crv\": "
				+ "\"P-256\",\"x\": \"6gkwNkBiu0gQHtXmJbOa-rvjCTmIsID5kIWZQMZRR3Y\",\"y\": \"ppDcilH7_X4Rf0tG9yt_9Ar_nTlWLAx3zgFHQxWPsEk\"}},\"timestamp\": \"2016-04-15T15:51:38\"}";
					//	ij-D2zX6fF0arOnMelPJ0zGE_98

		System.out.println(stringone.toString());
		HMac m=new HMac(new SHA1Digest());
	    m.init(new KeyParameter("The Best Secret Key".getBytes("UTF-8")));
	    byte[] bytes=b64coder.decode(stringone.toString());
	    m.update(bytes,0,bytes.length);
	    byte[] mac=new byte[m.getMacSize()];
	    m.doFinal(mac,0);
	    String signature=b64coder.encodeBase64URLSafeString(mac);		
	    
	    System.out.println(signature);
	}

}
