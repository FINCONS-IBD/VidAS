package com.fincons.videostorage.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FileManager {
	
	final static Logger logger = Logger.getLogger(FileManager.class);
	
	public static JSONObject getDirContent(String path, boolean onlyDir) throws JSONException {
		logger.info(path);
		JSONObject response= new JSONObject();
		String message="";
		int status=500;
		try{
			File pathOrganization= new File(path);
			if(!pathOrganization.exists()){
				status=404;
				message="Not Found";
			}else{
				JSONArray listVideos= new JSONArray();
				File[] listaFile=pathOrganization.listFiles();
				if(onlyDir){
					listaFile=pathOrganization.listFiles(new FilenameFilter() {
						  @Override
						  public boolean accept(File current, String name) {
						    return new File(current, name).isDirectory();
						  }
						});
				}
				logger.info(listaFile);
				for(File fileVideo:listaFile){
					logger.info(fileVideo.getName());
					JSONObject policy= new JSONObject();
					policy.put(Constants.NAME_VIDEO, URLDecoder.decode(fileVideo.getName(), "UTF-8"));
					if(fileVideo.isDirectory()){
						policy.put("URLDir", fileVideo.getName());
					}else{
						policy.put("URLVideo", pathOrganization.getName()+"/"+fileVideo.getName());
					}
					listVideos.put(policy);
				}
				response.put(Constants.LIST_VIDEO, listVideos);
				status=200;
				message="Ok";
			}
		} catch (JSONException e) {
			logger.error("IOException", e);
			status=500;
			message="Internal Server Error";
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException", e);
			status=500;
			message="Internal Server Error";
		}finally{
			response.put("code", status);
			response.put("message", message);
		}
		return response;
	}
	

}
