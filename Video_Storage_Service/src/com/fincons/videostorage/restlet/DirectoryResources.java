package com.fincons.videostorage.restlet;

import java.io.File;


import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fincons.videostorage.utils.Constants;
import com.fincons.videostorage.utils.FileManager;
import com.fincons.videostorage.utils.PropertiesHelper;
/**
 * 
 * @author diego.pedone
 * Not used because the directory are managed by NodeResources 
 */
@Deprecated
public class DirectoryResources extends ServerResource {
	final static Logger logger = Logger.getLogger(DirectoryResources.class);
	
	@Get
	/**
	 * 
	 * @author diego.pedone
	 * Not used because the directory are managed by NodeResources that use input and output in json format and use a DB to storage the information
	 */
	@Deprecated
	public JsonRepresentation getDirectory()throws ResourceException {

		logger.info("Start getDirectory");
		String directory = (String)this.getRequestAttributes().get("dir");
		
		JSONObject jsonObjectRet = null;
		int status;
		try {

			String path=PropertiesHelper.getProps().getProperty(Constants.PATH_FILE);
			if(directory==null){
				jsonObjectRet=FileManager.getDirContent(path, true);
			}else{
				jsonObjectRet=FileManager.getDirContent(path+"/"+directory, false);
			}
			status=jsonObjectRet.getInt("code");
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
			jsonObjectRet = new JSONObject();
			try {
				jsonObjectRet.put("faultCode", Status.SERVER_ERROR_INTERNAL);
				jsonObjectRet.put("message", "Internal Server Error");
			} catch (JSONException e1) {
				logger.error("JSONException", e1);
				e1.printStackTrace();
			}
			
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonObjectRet);
		}	

		getResponse().setStatus(new Status(status));
		logger.info("Key generation sucessfull completed.");
		return new JsonRepresentation(jsonObjectRet);

	}
	
	@Post
	/**
	 * 
	 * @author diego.pedone
	 * Not used because the directory are managed by NodeResources that use input and output in json format and use a DB to storage the information
	 */
	@Deprecated
	public JsonRepresentation saveDirectory(String parameters)throws ResourceException {

		logger.info("Start saveDirectory (json) : "+ parameters);
		String directory = (String)this.getRequestAttributes().get("dir");

		JSONObject jsonObjectRet = null;
		int status=500;
		String message="Internal Server Error";
		try {

//			String path=VideoResources.class.getClassLoader().getResource(PropertiesHelper.getProps().getProperty(Constants.PATH_FILE)).getPath();
			String path=PropertiesHelper.getProps().getProperty(Constants.PATH_FILE);
			
			new File(path).setWritable(true);
			File file=new File(path+"/"+directory); 
			if(file.exists()){
				status = 409;
				message="Conflit";
			}else if(file.mkdir()){
				status=201;
				message="Created";
			}else if(!file.canWrite()){
				message="Impossible write in the folder ";
				logger.error("Impossible write in the folder contact the administrator");
			}
			jsonObjectRet = new JSONObject();
			try {
				jsonObjectRet.put("code", status);
				jsonObjectRet.put("message", message);
			} catch (JSONException e1) {
				logger.error("JSONException", e1);
				e1.printStackTrace();
			}
							
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
			jsonObjectRet = new JSONObject();
			try {
				jsonObjectRet.put("faultCode", Status.SERVER_ERROR_INTERNAL);
				jsonObjectRet.put("message", "Internal Server Error");
			} catch (JSONException e1) {
				logger.error("JSONException", e1);
				e1.printStackTrace();
			}
			getResponse().setStatus(new Status(500));
			logger.info("Key generation sucessfull completed.");
			return new JsonRepresentation(jsonObjectRet);
		}	

		getResponse().setStatus(new Status(status));
		logger.info("Key generation sucessfull completed.");
		return new JsonRepresentation(jsonObjectRet);

	}

}
