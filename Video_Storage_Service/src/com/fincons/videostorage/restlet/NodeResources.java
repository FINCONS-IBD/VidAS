package com.fincons.videostorage.restlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import com.fincons.videostorage.dbmanager.ConstantsDB;
import com.fincons.videostorage.dbmanager.DBConnection;
import com.fincons.videostorage.utils.Constants;
import com.fincons.videostorage.utils.PropertiesHelper;

public class NodeResources  extends ServerResource {
	final static Logger logger = Logger.getLogger(NodeResources.class);
	
	@Post("json")
	public JsonRepresentation saveJsonEncryptedVideo(JsonRepresentation entity)throws Exception {
		
		JSONObject jsonObjectRet = new JSONObject();
		int status = 500;
		
		JSONObject jsonRequest=entity.getJsonObject();
		String org_name=jsonRequest.optString("folderChoose");
		System.out.println(org_name);
		
		Map<String, Object> params = new HashMap<String, Object>();
		
		if(jsonRequest.has("file")){
			JSONObject jsonEncryptedFile=jsonRequest.getJSONObject("file");
			String nameEncFile=jsonEncryptedFile.optString("name_enc_file");
			String name_enc_file=nameEncFile+PropertiesHelper.getProps().getProperty(Constants.EXT_CRYPTED_FILE);
			
			String dir = PropertiesHelper.getProps().getProperty(Constants.PATH_FILE);
			
			JSONArray fileMetaddata=jsonEncryptedFile.optJSONArray("metadata");
			
			//metadata pamareter
			for (Object object : fileMetaddata) {
				if(object instanceof JSONObject){
					JSONObject json=(JSONObject)object;
					params.put("fm_"+json.optString("name"),json.optString("value"));
				}
			}
			//get path file from metadata
			String sub_path_file=(String) params.get("fm_path_file");
			//if path is null the request is not accettable
			if(sub_path_file==null || sub_path_file==""){
				jsonObjectRet.put("code", Status.SERVER_ERROR_INTERNAL);
				jsonObjectRet.put("message", "Invalid Path");
				getResponse().setStatus(new Status(500));
				return new JsonRepresentation(jsonObjectRet);
			}
			
			//create sub folder in path in organization folder
			String fileDirPath=org_name+"/"+sub_path_file;
			logger.debug(fileDirPath);
			Files.createDirectories(Paths.get(dir+"/"+fileDirPath));
			int currentSliceEncSize=0;
			//file Append
			boolean flagCreation=false;
			String fullPathEncFile=dir+"/"+fileDirPath+"/"+name_enc_file;
			try (FileOutputStream file = new FileOutputStream(fullPathEncFile, true)){
				String encFileB64=jsonEncryptedFile.optString("encfile");
				byte[] encFileBA=Base64.getUrlDecoder().decode(encFileB64);
				currentSliceEncSize= encFileBA.length;
//				params.put("sizeFileEnc", encFileBA.length);
				file.write(encFileBA);
//				file.write(jsonEncryptedFile.optString("encfile").toString());
				logger.debug("file write");
				jsonEncryptedFile.remove("encfile");
				flagCreation=true;
			}catch(Exception e){
				logger.error(e);
				e.printStackTrace();
			}
			
			logger.info("append FileSlice number"+jsonEncryptedFile.optInt("currentSlice"));
			
			//the last slice store the metadata in DB
			if(jsonEncryptedFile.optInt("currentSlice")==jsonEncryptedFile.optInt("totalSlice")-1){
				//Calculate generic  size of Slice Encrypted
				File encFile=new File(fullPathEncFile);
				int sizeFileCompletateEnc=(int)encFile.length();
				encFile=null;
				logger.debug(jsonEncryptedFile.optInt("currentSlice") +"/"+ jsonEncryptedFile.optInt("totalSlice") );
				logger.debug(sizeFileCompletateEnc +" "+currentSliceEncSize );
//				logger.debug((sizeFileCompletateEnc-currentSliceEncSize)/(jsonEncryptedFile.optInt("totalSlice")-1));
				int genericSliceEncSize=sizeFileCompletateEnc;
				if(jsonEncryptedFile.optInt("totalSlice")>1){
					genericSliceEncSize=(sizeFileCompletateEnc-currentSliceEncSize)/(jsonEncryptedFile.optInt("totalSlice")-1);
				}
				params.put("sizeFileEnc",genericSliceEncSize);
				
				//Create in DB the Folder Tree
				DBConnection.getInstance().createFolderTree(jsonEncryptedFile.optString("encryptor"), fileDirPath);
				
				//Get Key-Info(Storage and Key info Metadata)
				JSONObject key_info=jsonEncryptedFile.optJSONObject("key_info");
				JSONObject storage=key_info.optJSONObject("storage");
				
				//add metaInfo
				JSONArray metadata=key_info.optJSONArray("metadata");
				for (Object object : metadata) {
					if(object instanceof JSONObject){
						JSONObject json=(JSONObject)object;
						params.put("keyInfo_metadata_"+json.optString("name"),json.optString("value"));
					}
				}
				// add enc_sym_key_id
				params.put("enc_sym_key_id", key_info.optString("enc_sym_key_id"));
				// add storage_type
				params.put("storage_type", storage.optString("storage_type"));
				// add storage_parameters
				JSONArray storageParameters=storage.optJSONArray("storage_parameters");
				for (Object object : storageParameters) {
					if(object instanceof JSONObject){
						JSONObject json=(JSONObject)object;
						params.put("keyInfo_sp_"+json.optString("name"),json.optString("value"));
					}
				}
				
				// add other file parameters
				params.put("iv", jsonEncryptedFile.optString("iv"));
				params.put("encryptor", jsonEncryptedFile.optString("encryptor"));
				params.put("name", jsonEncryptedFile.optString("name"));
				params.put("type", jsonEncryptedFile.optString("type_file"));
				params.put("sizeSlice", jsonEncryptedFile.optInt("sizeSlice"));
				params.put("sizeFile", jsonEncryptedFile.optInt("sizeFile"));
				params.put("totalSlice", jsonEncryptedFile.optInt("totalSlice"));
				params.put("real_path_file", fullPathEncFile);
			
				//Store metadata in DB
				flagCreation=DBConnection.getInstance().createFile(params, fileDirPath);
				
			}
			
			if(flagCreation){
				status=201;
				jsonObjectRet.put("code", status);
				jsonObjectRet.put("message", "Create File Encrypted");
			}else{
				status=500;
				jsonObjectRet.put("code", status);
				jsonObjectRet.put("message", "Error in encrypted file storage");
			}
		}
	
		getResponse().setStatus(new Status(status));
		return new JsonRepresentation(jsonObjectRet);
	}
	
	@Delete()
	public JsonRepresentation deleteElement() throws Exception {
		logger.info("Start Delete from DB");

		JSONObject response = null;
		String filePath = this.getRequest().getResourceRef().toString();
		String toReplace = this.getRequest().getResourceRef().getBaseRef().toString();
		
		String path = filePath.replaceAll(toReplace, "/").replaceFirst("/", "");
		String pathUrlDecode= URLDecoder.decode(path, "utf-8");
		
		//remove get parameter (currentSlice from url
		if(pathUrlDecode.contains("currentSlice="))
			pathUrlDecode= pathUrlDecode.substring(0 , pathUrlDecode.lastIndexOf("?"));
		
		logger.debug("path Url File"+pathUrlDecode);
		
		int status = 500;
		response = new JSONObject();
		String classNode="";
		JSONObject lastNode=null;
		
		//Check organization in the Url 
		if(checkOrganization()){
			//Get the last node in the path
			lastNode = DBConnection.getInstance().getLastNodeFromRelativePath(pathUrlDecode);
			if(lastNode!=null && lastNode.has(ConstantsDB.CLASS_NODE)){
				classNode=lastNode.getString(ConstantsDB.CLASS_NODE);
			}
			if(lastNode==null){
				status=404;
			}
		}
		JSONObject subNode = DBConnection.getInstance().deleteNodeById(lastNode.optString(ConstantsDB.ID_NODE));
		if(subNode.optString("status").equalsIgnoreCase("ok")){
			status = 200;
			logger.info("Get element from DB sucessfull completed.");
		} else {
			status = 501;
			logger.error("Element could not be deleted");
		}
		response.put("code", status);
		getResponse().setStatus(new Status(status));
		return new JsonRepresentation(response);
	}
	
	@Get("json")
	public JsonRepresentation getElement() throws Exception {
		logger.info("Start getVideo from DB");

		JSONObject response = null;
		String filePath = this.getRequest().getResourceRef().toString();
		String toReplace = this.getRequest().getResourceRef().getBaseRef().toString();
		
		String path = filePath.replaceAll(toReplace, "/").replaceFirst("/", "");
		String pathUrlDecode= URLDecoder.decode(path, "utf-8");
		
		//remove get parameter (currentSlice from url
		if(pathUrlDecode.contains("currentSlice="))
			pathUrlDecode= pathUrlDecode.substring(0 , pathUrlDecode.lastIndexOf("?"));
		
		logger.debug("path Url File"+pathUrlDecode);
		
		int status = 500;
		response = new JSONObject();
		String classNode="";
		JSONObject lastNode=null;
		
		//Check organization in the Url 
		if(checkOrganization()){
			//Get the last node in the path
			lastNode = DBConnection.getInstance().getLastNodeFromRelativePath(pathUrlDecode);
			if(lastNode!=null && lastNode.has(ConstantsDB.CLASS_NODE)){
				classNode=lastNode.getString(ConstantsDB.CLASS_NODE);
			}
			if(lastNode==null){
				status=404;
			}
		}
		
		switch(classNode){
		case ConstantsDB.DIRECTORY_CLASS:
			logger.info("case VideoDir");
			JSONArray subNode = DBConnection.getInstance().getSubNode(lastNode.optString(ConstantsDB.ID_NODE));
			status = 200;
			response.put("code", status);
			response.put("listVideo", createListElement(subNode, path));
			break;
		case ConstantsDB.VIDEO_CLASS:
			logger.info("case Video");
			String pathFile= lastNode.optString("real_path_file");
			
			//get from url the currentSlice number
			Form queryParams = getRequest().getResourceRef().getQueryAsForm();
			int currentSlice=  0;
			try{
				currentSlice=Integer.parseInt(queryParams.getFirstValue("currentSlice"));
			}catch(Exception e){
				logger.warn(e);
			}
			int sizeFileEnc=lastNode.optInt("sizeFileEnc");
			
			//get the file from pathFile 
			File encFile = new File(pathFile);
			long fileSize= encFile.length();
			InputStream is = new FileInputStream(encFile);
			encFile=null;
			//read the currentSlice from file 
			int lengthCurrentSlice= (int) ((sizeFileEnc*(currentSlice+1)>fileSize) ? fileSize-sizeFileEnc*(currentSlice): sizeFileEnc);
			byte currentSliceBA[] = new byte[lengthCurrentSlice];
			is.skip(sizeFileEnc*currentSlice);
			is.read(currentSliceBA,0, lengthCurrentSlice);
			is.close();
			//encode byte array currentSlice in Base64URl
			String jsonTxt=Base64.getUrlEncoder().withoutPadding().encodeToString(currentSliceBA);
			// add
			lastNode.put("encfile", jsonTxt);
			lastNode.put("currentSlice", currentSlice);
			
			//CREATE JSON KEY_INFO OBJECT
			JSONObject key_info= new JSONObject();
			JSONArray metadata= new JSONArray();
			JSONArray storage_parameters= new JSONArray();
			
			//set JSON KEY_INFO by storaage and key info metadata
			List<String> keysToRemove= new ArrayList<String>();
			for (String key : lastNode.keySet()) {
				if(key.contains("keyInfo_")){
					if(key.contains("_metadata_")){
						String name=key.substring("keyInfo_metadata_".length(), key.length());
						metadata.put(new JSONObject().put("name", name).put("value",lastNode.opt(key)));
						keysToRemove.add(key);					
					}else if(key.contains("_sp_")){
						String name=key.substring("keyInfo_sp_".length(), key.length());
						storage_parameters.put(new JSONObject().put("name", name).put("value",lastNode.opt(key)));
						keysToRemove.add(key);					
					}
				}
			}
			key_info.put("enc_sym_key_id", lastNode.optString("enc_sym_key_id"));		
			JSONObject storage= new JSONObject();
			storage.put("storage_type", lastNode.opt("storage_type"));
			storage.put("storage_parameters", storage_parameters);
			
			key_info.put("storage", storage);
			key_info.put("metadata",  metadata);
			lastNode.put("key_info", key_info);
			

			//remove duplicate Key_info parameter
			lastNode.remove("enc_sym_key_id");
			lastNode.remove("storage_type");
			for (String keyToRemove : keysToRemove) {
				lastNode.remove(keyToRemove);
			}
			
			status = 200;
			response.put("code", status);
			response.put("encryptedFile", lastNode);
			break;
		default:
			logger.info("default "+ classNode);
//			status = status;
			response.put("code", status);
			response.put("message", "Element not Found");
			break;
		}		
		getResponse().setStatus(new Status(status));
		logger.info("Get element from DB sucessfull completed.");
		return new JsonRepresentation(response);
	}
	
	private boolean checkOrganization() throws Exception {
		Series headers = (Series) this.getRequest().getAttributes().get("org.restlet.http.headers");
		String user_token = headers.getFirstValue("authorization");
		JwtConsumer jwtConsumerSkipSign = new JwtConsumerBuilder()
		        .setDisableRequireSignature()
		        .setSkipSignatureVerification()
		        .build();
		JwtClaims jwtExternalClaims = jwtConsumerSkipSign.processToClaims(user_token);
		JwtClaims jwtInternalClaims = jwtConsumerSkipSign.processToClaims(jwtExternalClaims.getClaimValue("token").toString());
		JSONObject	jsonClaim = new JSONObject(jwtInternalClaims.toJson());
		JSONObject action= jsonClaim.optJSONObject("action");
		String myURL= action.optString("url");
		String org= myURL.split("/")[0];
		if(org!=null && !org.equals("")){
			
			JSONObject result=DBConnection.getInstance().createFolderTree("VideoService", org);
			if(result!=null){
				return true;
			}
		}
		return false;
	}


	/**
	 * Creates a list in JSON format with the elements inside the folder parameter.
	 * This method takes the JSON Object with the informations about the content of an OrientDB node and extract a list of elements
	 * in JSON Array format.
	 * @param folderContent
	 * 		The JSON Object result of the query.
	 * @return JSON Array with the elements contained in the folder node
	 */
	private JSONArray createListElement(JSONArray folderContent, String path) {
		JSONArray list = null;
		try {
			list = new JSONArray();
			logger.debug("Get content of the folder requested");
			folderContent=(folderContent==null)? new JSONArray():folderContent;

			for (int i = 0; i < folderContent.length(); i++) {
				JSONObject dataObj = folderContent.optJSONObject(i);
				JSONObject temp = new JSONObject();
				if (dataObj.optString(ConstantsDB.CLASS_NODE).equalsIgnoreCase(ConstantsDB.DIRECTORY_CLASS)) {
					String name= dataObj.optString("name");
					String fullPath = path+"/"+name;
					if(fullPath.startsWith("//")){
						fullPath = fullPath.replaceFirst("//", "");
					} else if(fullPath.startsWith("/")){
						fullPath = fullPath.replaceFirst("/", "");
					}
					temp.put(Constants.NAME_VIDEO, name);
					temp.put("URLDir", fullPath); //(path+"/"+name).replace("//", "")
				} else if (dataObj.optString(ConstantsDB.CLASS_NODE).equalsIgnoreCase(ConstantsDB.VIDEO_CLASS)) {
					String name= dataObj.optString("name");
					String fullPath = path+"/"+name;
					if(fullPath.startsWith("//")){
						fullPath = fullPath.replaceFirst("//", "");
					} else if(fullPath.startsWith("/")){
						fullPath = fullPath.replaceFirst("/", "");
					}
//					String nameShowed= dataObj.optString("fm_filename");
					temp.put(Constants.NAME_VIDEO, name);
					temp.put("URLVideo", fullPath); //(path+"/"+name).replace("//", "")
				}
				list.put(temp);
			}
		} catch (JSONException e) {
			logger.error("JSON Exception " + e.getMessage());
			return list;
		}
		return list;
	}
}
