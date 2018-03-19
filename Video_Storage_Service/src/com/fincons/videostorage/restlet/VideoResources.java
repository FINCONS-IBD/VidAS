package com.fincons.videostorage.restlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fincons.rabbitmq.client.RabbitMqClient;
import com.fincons.rabbitmq.client.factory.RequestFactory;
import com.fincons.rabbitmq.publisher.Publisher;
import com.fincons.util.ApplicationPropertiesRepository;
import com.fincons.videostorage.rabbitmq.PublishingService;
import com.fincons.videostorage.utils.Constants;
import com.fincons.videostorage.utils.PropertiesHelper;
/**
 * 
 * @author diego.pedone
 * Not used because the Video are managed by NodeResources that use input and output in json format and use a DB to storage the information
 */
@Deprecated
public class VideoResources extends ServerResource {
final static Logger logger = Logger.getLogger(VideoResources.class);


	@Post
	/**
	 * 
	 * @author diego.pedone
	 * Not used because the Video are managed by NodeResources that use input and output in json format and use a DB to storage the information
	 */
	@Deprecated
	public JsonRepresentation saveVideo(Representation entity)throws ResourceException {
		logger.info("Start saveVideo (entity) ");
		String directory = (String)this.getRequestAttributes().get("dir");
		String nameFile= (String)this.getRequestAttributes().get("nameFile");
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(1000240);
		RestletFileUpload upload = new RestletFileUpload(factory);
		JSONObject jsonObjectRet = new JSONObject();
		File file=null;
		int status;
		
		List<String> analisysServicesList= new ArrayList<String>();
		try {
			FileItemIterator fileIterator = upload.getItemIterator(entity);
			while (fileIterator.hasNext()) {
				
				FileItemStream fi = fileIterator.next();
				if(fi.getFieldName().equals("crypted")){
//					String pathFile = JsonRepresentation.class.getClassLoader()
//							.getResource(PropertiesHelper.getProps().getProperty(Constants.PATH_FILE)).getPath()+directory+"/"+ nameFile+
//							PropertiesHelper.getProps().getProperty(Constants.EXT_CRYPTED_FILE);
					String pathFile = PropertiesHelper.getProps().getProperty(Constants.PATH_FILE)+"/"+directory+"/"+ nameFile+
							PropertiesHelper.getProps().getProperty(Constants.EXT_CRYPTED_FILE);
					file = new File(pathFile);
					if(file.exists()){
						jsonObjectRet.put("code", 409);
						jsonObjectRet.put("message", "File Conflict");
						break;
					}
					InputStream is = fi.openStream();
					OutputStream oos = new FileOutputStream(file);
					byte[] buf = new byte[fi.openStream().available()];
					int c = 0;
					while ((c = is.read(buf, 0, buf.length)) > 0) {
						oos.write(buf, 0, c);
						oos.flush();
					}
					oos.close();
					logger.info("File upload completed.");
					is.close();
					
				}else{
					if(fi.getFieldName().contains("AnalysisService-")){
						BufferedReader br = new BufferedReader(new InputStreamReader(fi.openStream()));
						StringBuilder sb = new StringBuilder();
						String line = null;
						while ((line = br.readLine()) != null) {
							sb.append(line);
							sb.append("\n");
						}
						analisysServicesList.add(sb.toString().trim());
					}
				}
			}
			if(file.exists()){
				jsonObjectRet.put("code", 201);
				jsonObjectRet.put("message", "File Created and Crypted");
				for(String service: analisysServicesList){
					RabbitMqClient client = RequestFactory.startGuestApplication(ApplicationPropertiesRepository.PUBLISH);
					if(client.isConnected() && client instanceof Publisher){
						PublishingService publishingRunnable = new PublishingService((Publisher)client,file.getAbsolutePath(), service);
						Thread publishingThread = new Thread(publishingRunnable);
			            if(client.isConnected())
			            	publishingThread.start();		
					}
				}
				
				
				
			}else{
				jsonObjectRet.put("code", 500);
				jsonObjectRet.put("message", "Error File not is created");
			}
			status=jsonObjectRet.getInt("code");
			
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
			jsonObjectRet = new JSONObject();
			try {
				jsonObjectRet.put("code", Status.SERVER_ERROR_INTERNAL);
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
	

	@Get
	/**
	 * 
	 * @author diego.pedone
	 * Not used because the Video are managed by NodeResources that use input and output in json format and use a DB to storage the information
	 */
	@Deprecated
	public Representation getVideo()throws ResourceException {

		logger.info("Start getVideo");
		String directory = (String)this.getRequestAttributes().get("dir");
		String nameVideo= (String)this.getRequestAttributes().get("nameFile");
		Representation resp=null;
		JSONObject jsonObjectRet = null;
		try {

//			String path=VideoResources.class.getClassLoader().getResource(PropertiesHelper.getProps().getProperty(Constants.PATH_FILE)).getPath();
			String path=PropertiesHelper.getProps().getProperty(Constants.PATH_FILE);
				
			File fileCrypted = new File(path+"/"+directory+"/"+nameVideo);
			if(fileCrypted.exists()){	
				resp = new FileRepresentation(fileCrypted, MediaType.TEXT_PLAIN);
				resp.getDisposition().setType(Disposition.TYPE_ATTACHMENT);
				getResponse().setStatus(new Status(200));
				logger.info("Video found");
				return resp;
			}else{
				logger.info("Video not found");
				jsonObjectRet = new JSONObject();
				jsonObjectRet.put("code", Status.CLIENT_ERROR_NOT_FOUND);
				jsonObjectRet.put("message", "Video Not Found");
				getResponse().setStatus(new Status(404));
				return new JsonRepresentation(jsonObjectRet);
			}
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
			jsonObjectRet = new JSONObject();
			try {
				jsonObjectRet.put("code", Status.SERVER_ERROR_INTERNAL);
				jsonObjectRet.put("message", "Internal Server Error");
			} catch (JSONException e1) {
				logger.error("JSONException", e1);
				e1.printStackTrace();
			}
			
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(jsonObjectRet);
		}

	}
}
