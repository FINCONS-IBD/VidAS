package com.fincons.restlet.proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.log4j.Logger;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.ext.html.FormData;
import org.restlet.ext.html.FormDataSet;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import com.fincons.utils.Constants;
import com.fincons.utils.CpabeManager;
import com.fincons.utils.PropertiesHelper;
import com.fincons.utils.Util;

public class EncryptionServerResource extends ServerResource {

	final static Logger logger = Logger.getLogger(EncryptionServerResource.class);

	@Post
	public Representation handleUpload(Representation entity) {
		
		logger.info("Called the encryptProxy method to encrypt uploaded file");
		
		if (entity != null && MediaType.MULTIPART_FORM_DATA.equals(entity.getMediaType(), true)) {
//			String username = getRequest().getClientInfo().getUser().getIdentifier();
			DiskFileItemFactory factory = new DiskFileItemFactory();
//			factory.setSizeThreshold(1000240);
			RestletFileUpload upload = new RestletFileUpload(factory);
			Representation serverResponse=null;
			String fileName = "";
//			FileRepresentation resp = null;
			String pathFile = "";
			String pathFileCrypted = "";
			try {
				FileItemIterator fileIterator = upload.getItemIterator(entity);
				String policy = "";
				Map<String , String> fieldList = new HashMap<String, String>();
				while (fileIterator.hasNext()) {
					FileItemStream fi = fileIterator.next();

					String fieldName = fi.getFieldName();
			
					if (!("upfile".equals(fi.getFieldName()))) {
						BufferedReader br = new BufferedReader(new InputStreamReader(fi.openStream()));
						StringBuilder sb = new StringBuilder();
						String line = null;
						while ((line = br.readLine()) != null) {
							sb.append(line);
							sb.append("\n");
						}
						fieldList.put(fieldName,sb.toString().trim());
					} else {
						

						fileName = fi.getName().replace(" ", "_").trim();

						pathFile = EncryptionServerResource.class.getClassLoader()
								.getResource(PropertiesHelper.getProps().getProperty(Constants.PATH_FILE)).getPath() + "-"  + fileName;
						pathFileCrypted = pathFile + PropertiesHelper.getProps().getProperty(Constants.EXTENSION_CRYPTED_FILE);

						File file = new File(pathFile);
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
						
						Util.dropOldFile();
						
//						BufferedWriter bwr = new BufferedWriter(new FileWriter(file));
//						bwr.write(sb.toString());
//						bwr.flush();
//						bwr.close();
					}
				}
			    FormDataSet fds = new FormDataSet(); //create the FormDataSet
				policy= fieldList.get("policyText");
//				List<String> analisysServices= new ArrayList<String>();
				for (String key:fieldList.keySet()){
					if(key.contains("AnalysisService-")){
//						analisysServices.add(fieldList.get(key));
					  //create the Formdata using a key and a value (file)
					    fds.getEntries().add( new FormData(key,fieldList.get(key)));
					}
				}
//				logger.debug("listService: "+ analisysServices);
				CpabeManager cpabeManager = new CpabeManager();
				logger.debug("policy: "+ policy);
				String pubKey=  fieldList.get("publicKey");
				logger.debug("pubKey: "+ pubKey);
				cpabeManager.enc(pubKey, policy, pathFile, pathFileCrypted);

				
				
				File fileCrypted = new File(pathFileCrypted);
				
				
//				resp = new FileRepresentation(fileCrypted, MediaType.APPLICATION_PDF);
//				resp.getDisposition().setType(Disposition.TYPE_ATTACHMENT);
				ClientResource clientResource = null;

				Client client = new Client(new Context(), Protocol.HTTP);
				client.getContext().getParameters().add("socketTimeout", "600000");
				//move the Auth JWT Token from the first request to second request
				Series<Header> firs_headers =  (Series<Header>) getRequestAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
				String user_token = firs_headers.getFirstValue("authorization");
				
				JwtConsumer jwtConsumerSkipSign = new JwtConsumerBuilder()
		        .setDisableRequireSignature()
		        .setSkipSignatureVerification()
		        .build();
				String method="";
				String url="";
				try {
					JwtClaims jwtExternalClaims = jwtConsumerSkipSign.processToClaims(user_token);
					JwtClaims jwtInternalClaims = jwtConsumerSkipSign.processToClaims(jwtExternalClaims.getClaimValue("token").toString());
					JSONObject	jsonClaim = new JSONObject(jwtInternalClaims.toJson());
					JSONObject action= jsonClaim.optJSONObject("action");
					method= action.optString("method");
					String server= PropertiesHelper.getProps().getProperty(action.optString("service"));
					url= server+"/"+ action.optString("url");
				} catch (InvalidJwtException e) {
					logger.error("Invalid Token", e);
					e.printStackTrace();
					getResponse().setStatus(new Status(401));
					return new JsonRepresentation(getJsonError(401, "Invalid Token"));
				} catch (JSONException e) {
					logger.error("Error Json", e);
					e.printStackTrace();
					getResponse().setStatus(new Status(500));
					return new JsonRepresentation(getJsonError(500, "Error in JSON parsing"));
				}
			
				logger.debug("Request forward: "+ url +" with Method"+ method);	
				
				clientResource = new ClientResource(url);
				Series<Header> requestHeaders = new Series(Header.class); 
				requestHeaders.add(new Header("Authorization", user_token)); //N.B. the add accept only standard HTTP Headers
				clientResource.getRequestAttributes().put("org.restlet.http.headers", requestHeaders); 

				clientResource.setNext(client);				
				clientResource.setMethod(Method.POST);
				
				
//				Representation rep=new (MediaType.MULTIPART_FORM_DATA);
				FileRepresentation fileRep = new FileRepresentation(fileCrypted, MediaType.TEXT_PLAIN); //create the fileRepresentation  
			
			    FormData fd = new FormData("crypted", fileRep); //create the Formdata using a key and a value (file)
			 
			    fds.getEntries().add(fd); //add the form data to the set
			    fds.setMultipart(true);
				
				serverResponse = clientResource.post(fds);
				logger.info("Start file encrypted download.");

			} catch (FileUploadException e) {
				logger.error("FileUploadException", e);
				e.printStackTrace();
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
			}  catch (Exception e) {
				logger.error("Exception", e);
				e.printStackTrace();
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
			}

			return serverResponse;

		} else {
			logger.error("CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE");
			throw new ResourceException(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);
		}
	}
	
	public static JSONObject getJsonError(int code, String message){
		JSONObject errorJson= new JSONObject();
		try {
			errorJson.put("code", code);
			errorJson.put("message", message);
		} catch (JSONException e) {
			logger.error("Error in create ErrorJson code:"+code +"message:'"+message+"'.",e );
		}finally{
			return errorJson;
		}
	}
}