package com.fincons.keygenerator.restlet;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import com.fincons.keygenerator.utils.Constants;
import com.fincons.keygenerator.utils.CpabeManager;
import com.fincons.keygenerator.utils.PropertiesHelper;

import cpabe.Common;

public class PublicKeyResource extends ServerResource {

	final static Logger logger = Logger.getLogger(PublicKeyResource.class);
	
	@Get
	public JsonRepresentation getPublicKey() {
		int status=500;
		logger.info("Called the getPulicKey method");
		
		JSONObject jsonObjectRet = new JSONObject();
		String key_dir_name = PropertiesHelper.getProps().getProperty(Constants.CPABE_KEY_PATH_NAME);
		String public_key_name = PropertiesHelper.getProps().getProperty(Constants.CPABE_PUBLIC_KEY_NAME);
//		String key_path = CpabeManager.class.getClassLoader().getResource(key_dir_name).getPath();
		
//		File f_p = new File(key_path+public_key_name);
		File f_p = new File(key_dir_name+public_key_name);
		logger.info("path key" +f_p.getAbsolutePath());
		if(f_p.exists()){
			try {
//				byte[] pub_byte = Common.suckFile(key_path+public_key_name);
				byte[] pub_byte = Common.suckFile(key_dir_name+public_key_name);
				Base64 b64enc= new Base64(true);
				String publicKeyString= b64enc.encodeAsString(pub_byte);
				jsonObjectRet.put("code", 200);
				jsonObjectRet.put("publicKey",publicKeyString);
				status=200;
			} catch (IOException e) {
				logger.error("IOException", e);
				try {
					jsonObjectRet.put("faultCode", 500);
					jsonObjectRet.put("message", "Error In decode key");
				} catch (JSONException e1) {
					logger.error("JSONException", e1);
					e1.printStackTrace();
				}
			} catch (JSONException e) {
				try {
					jsonObjectRet.put("faultCode", 500);
					jsonObjectRet.put("message", "Error In generation JSON Response");
				} catch (JSONException e1) {
					logger.error("JSONException", e1);
					e1.printStackTrace();
				}
			}
		}
		else{
			try {
				status=404;
				jsonObjectRet.put("faultCode", Status.CLIENT_ERROR_NOT_FOUND);
				jsonObjectRet.put("message", "Publick Key Not Found");
			} catch (JSONException e1) {
				logger.error("JSONException", e1);
				e1.printStackTrace();
			}
			
		}
		
		getResponse().setStatus(new Status(status));
		logger.info("Key generation sucessfull completed.");
		return new JsonRepresentation(jsonObjectRet);

	}

}