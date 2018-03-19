package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import algorithms.AES;
import algorithms.CPABE;
import algorithms.Elliptic_Curve_Diffie_Hellman;
import algorithms.HMAC;
import cpabe.Common;
import messages.Enc_Personal_Info;
import messages.Enc_Personal_InfoSign;
import messages.Key_Storage;
import messages.Metadata;
import messages.OrientDB_Recovered_Key;
import messages.Personal_Info;
import messages.Storage_Parameters;

/*
 * @author Diego Pedone
 * @version 24/05/2017
 */

/**
 * Servlet implementation class Get_Shared_Secret
 */
public class Get_Shared_Secret extends HttpServlet {
	private static final long serialVersionUID = 1L;
	/**
	 * servletConfig is used for the  management of username and password of KeyStorageService Used with AbeProxy
	 */
	private static Map<String, String>servletConfig;
	
	private String id;
	private byte[] public_parameters;
	private int expirationTime;
	
//	private long time_aes;
//	private long time_cpabe;
	
	/* KGS */
	//private String kgs_ip;
	//private String kgs_port;
	
	/**
	 * @see Servlet#init(ServletConfig)
	 */public void init(ServletConfig config) throws ServletException {
			try {
				/* Read parameters from the configure file */
				//ServletConfig conf=getServletConfig();
				//this.kgs_ip = config.getInitParameter("kgs_ip");
				//this.kgs_port = config.getInitParameter("kgs_port");
				
				TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
//				this.time_aes = 0;
//				this.time_cpabe = 0;
				
				this.id = config.getServletContext().getInitParameter("proxy_id");
			
				Enumeration<String> configParams=config.getServletContext().getInitParameterNames();
//				String param =configParams.nextElement();
				/**
				 * servletConfig is populated from all InitParameter in web.xml
				 */
				if(servletConfig==null){
					servletConfig=new HashMap<String,String>();
					while (configParams.hasMoreElements()) {
					    String param = configParams.nextElement();
						servletConfig.put(param, config.getServletContext().getInitParameter(param));
					}
				}
				this.expirationTime =  Integer.parseInt(config.getServletContext().getInitParameter("expiration_time"));
				
				/* X.X. Request KGS's public parameters */
				if(this.public_parameters == null){
					//this.public_parameters = this.get_KGS_publik_key();
					String public_parameters_file = config.getServletContext().getRealPath("/WEB-INF/cpabe_keys/pub_10"); //example
					this.public_parameters = Common.suckFile(public_parameters_file);
				}
				/* X.X. Request KGS's public parameters */
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {

			
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			
			BufferedReader in = new BufferedReader(request.getReader());
			String line;
			String payload = "";
	        while ((line = in.readLine()) != null) {
	        	payload += line;
//	        	System.out.println(line);
	        }
	        
/* 5.3. Decrypt CP-ABE related information by AES (shared_sym_key) */
	        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
	        Enc_Personal_InfoSign enc_personal_info = gson.fromJson(payload, Enc_Personal_InfoSign.class);
	        
	        /* Recovery the symmetric key associated with the smart device in this context */
	        String smart_device_id = "";
	        for(Metadata m : enc_personal_info.getMetadata()){
	        	if(m.getName().equals("encryptor"))
	        		smart_device_id = m.getValue();
	        }
	        if(smart_device_id.equals(""))
	        	throw new Exception("The \"encryptor\" metadata is not specified");
	        Elliptic_Curve_Diffie_Hellman ecdh = (Elliptic_Curve_Diffie_Hellman)request.getServletContext().getAttribute(smart_device_id);
	        if(ecdh == null)
	        	throw new Exception("The symmetric key associated with the smart device " + smart_device_id + " has not been established (PHASE 1)");
	        /* Recovery the symmetric key associated with the smart device in this context */
//	        System.out.println(enc_personal_info.getEnc_Personal_info());
	        byte[] enc_personal_info_byte = Base64.getUrlDecoder().decode(enc_personal_info.getEnc_Personal_info().getEnc_Personal());
//	        System.out.println("file"+Arrays.toString(enc_personal_info_byte));
	        byte[] protected_sym_key = ecdh.getShared_Sym_Key();
//	        System.out.println("key"+Arrays.toString(protected_sym_key));
	        
	        //Check Signature
	        byte[] key2= HMAC.generateHMACKeySha256(protected_sym_key);
	        String enc_personal_info_toSign_json=gson.toJson(enc_personal_info.getEnc_Personal_info());
	        byte[] hmac=HMAC.signHMACSHA256(enc_personal_info_toSign_json.getBytes(), key2);
	       
	        String hmacB64=Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
			if(!hmacB64.equals(enc_personal_info.getTag())){
	        	throw new Exception("Signature not verified");
			}
	        
	        
//	        long startTimeAES = System.currentTimeMillis();
	        byte[] iv = Base64.getUrlDecoder().decode(enc_personal_info.getEnc_Personal_info().getIv());
	    	
	        AES aes = new AES();
			byte[] personal_info_byte = aes.AES_decrypt(enc_personal_info_byte, protected_sym_key, iv);
			
//			long endTimeAES   = System.currentTimeMillis();
//			this.time_aes += endTimeAES - startTimeAES;
			
			String personal_info_string = new String(personal_info_byte);
//			System.out.println("CPABE_INFO: " + Arrays.toString(personal_info_byte));
			System.out.println("CPABE_INFO: " + personal_info_string);
			Personal_Info personal_info = gson.fromJson(personal_info_string, Personal_Info.class);
			
			/* Delete the symmetric key associated with the smart device in this context and all information about such symmetric key */
			request.getServletContext().removeAttribute(smart_device_id);
			
			/* Delete the symmetric key associated with the smart device in this context and all information about such symmetric key */
/* 5.3. Decrypt CP-ABE related information by AES (shared_sym_key) */
/* 5.4 Get shared_sym_key from Storage*/
			Key_Storage key_storage = personal_info.getEncrypted_symmetric_key_storage();
			String enc_sym_key_string=this.getEncSymKey(key_storage, personal_info.getIdSharedSymmetricKey());
		    String[] splitenc_sym_key_string=enc_sym_key_string.split(" ");
		    
//		    System.out.println("Get_Shared enc_sym_key_string"+enc_sym_key_string);
		    
			byte[] cph_b = Base64.getUrlDecoder().decode(splitenc_sym_key_string[0]);
		    byte[] aes_b = Base64.getUrlDecoder().decode(splitenc_sym_key_string[1]);
		    
		    byte[][]enc_shared_sym_keyByte={cph_b,aes_b};
		    
		    System.out.println("Array Byte cph_b enc_sym_key_string"+Arrays.toString(cph_b));
		    System.out.println("Array Byte aes_b enc_sym_key_string"+Arrays.toString(aes_b ));
	       
		    String personal_key = personal_info.getPersonalKey().getKey();
	        byte[] personal_key_byte = Base64.getUrlDecoder().decode(personal_key);
//	        long startTimeCPABE = System.currentTimeMillis();
	        
	        CPABE cpabe = new CPABE();
//	        byte[][] enc_sym_key = cpabe.CPABE_encrypt(public_parameters, policy, shared_sym_key);

System.out.println("public key"+ Arrays.toString(public_parameters));
System.out.println("personal key"+ Arrays.toString(personal_key_byte));
System.out.println("public enc_shared_sym_keyByte"+Arrays.toString(cph_b)+" \n "+Arrays.toString(aes_b));

	        byte[] dec_shared_sym_key = cpabe.CPABE_decrypt(public_parameters, personal_key_byte, enc_shared_sym_keyByte);
		        
//	        long endTimeCPABE   = System.currentTimeMillis();
//			this.time_cpabe += endTimeCPABE - startTimeCPABE;
	        
	        if(dec_shared_sym_key==null)
	        	throw new Exception("Error decrypting symmetric key");
/* 2.6. Encrypt shared_sym_key with CP-ABE */	        
//	        String shared_sym_key = Base64.getUrlEncoder().withoutPadding().encodeToString(dec_shared_sym_key);
	        byte[] ivenc=  SecureRandom.getSeed(16);
	        String ivencB64= Base64.getUrlEncoder().encodeToString(ivenc);
	        
	        byte[] shared_sym_key_protected = aes.AES_encrypt(dec_shared_sym_key, protected_sym_key, ivenc);

	        if(shared_sym_key_protected==null)
	        	throw new Exception("Error encripting symmetric key");	        
	        String shared_sym_key_protected_string = Base64.getUrlEncoder().withoutPadding().encodeToString(shared_sym_key_protected);
	  
/* 2.9. Send the enc_sym_key_id to the smart device */
//			ArrayList<Metadata> key_id_metadatas = new ArrayList<Metadata>();
//			key_id_metadatas.add(encryption_date_metadata);key_id_metadatas.add(encryption_algorithms_metadata);key_id_metadatas.add(encryptor_metadata);key_id_metadatas.add(expiration_date_metadata);
//			Key_Id key_id = new Key_Id(enc_sym_key_id, key_storage, key_id_metadatas);
	        
	        JsonObject jsonEncContent= new JsonObject();
	        jsonEncContent.addProperty("shader_sym_key_protect", shared_sym_key_protected_string);
	        jsonEncContent.addProperty("iv", ivencB64);
	        byte[] jsonToSign = jsonEncContent.toString().getBytes();
//	        System.out.println("jsonToSign : " +Arrays.toString(jsonToSign) );
//
//	        System.out.println("key2 : " +Arrays.toString(key2) );
	        byte[] tag= HMAC.signHMACSHA256(jsonToSign, key2);
//	        System.out.println("tag : " +Arrays.toString(tag) );
	        String tagB64= Base64.getUrlEncoder().encodeToString(tag);
//	        System.out.println("tagB64: " +tagB64);
	        JsonObject jsonResponse= new JsonObject();
	        jsonResponse.add("json_encripted_key", jsonEncContent);
	        jsonResponse.addProperty("tag", tagB64);
	        String shared_sym_key_protected_json=jsonResponse.toString();
	        response.setStatus(HttpServletResponse.SC_OK);
			response.setContentLength(shared_sym_key_protected_json.length());
			OutputStream os = response.getOutputStream();
			os.write(shared_sym_key_protected_json.getBytes());
			os.close();
/* 2.9. Send the enc_sym_key_id to the smart device */

			/*  TEST 1: CONSUMED MEMORY: ABE PROXY */
//			long endTime   = System.currentTimeMillis();
//			runtime = Runtime.getRuntime();
//			runtime.gc();
//			long memory = runtime.totalMemory() - runtime.freeMemory();
//			long totalTime = endTime - startTime;
//			try {
//				File TextFile = new File("mem_pub_enc_proxy.csv"); 
//				FileWriter TextOut = new FileWriter(TextFile, true);
//				TextOut.write(memory/1024L + "\n");
//				TextOut.close();
//				TextFile = new File("time_pub_enc_proxy.csv"); 
//				TextOut = new FileWriter(TextFile, true);
//				TextOut.write(totalTime + ";");
//				TextOut.write(this.time_aes + ";");
//				TextOut.write(this.time_cpabe + "\n");
//
//				TextOut.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}
	
/***********************************/
/********* PRIVATE METHODS *********/
/***********************************/

//	private byte[] get_KGS_publik_key() throws Exception{
///* 2.4. Request KGS's public key */
//		String url_str = "http://" + this.kgs_ip + ":" + this.kgs_port + "/get_KGS_public_key";
//		URL url = new URL(url_str);
//		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//		connection.setDoOutput(true);
///* 2.4. Request KGS's public key */
//	
///* 2.5. Get KGS's public key */
//		InputStreamReader isrResponse = new InputStreamReader(connection.getInputStream());
//		BufferedReader brResp = new BufferedReader(isrResponse);
//		String pub_key_string = "", line;
//        while ((line = brResp.readLine()) != null) {
//        	pub_key_string += line;
//        	//System.out.println(line);
//        }
//        Gson gson = new Gson();
//        CPABE_Public_Key KGS_public_key_class = gson.fromJson(pub_key_string, CPABE_Public_Key.class);
//        byte[] public_parameters = Base64.getDecoder().decode(KGS_public_key_class.getPublic_Parameters());
///* 2.5. Get KGS's public key */
//        
//        return public_parameters;
//	}
	
    private String getEncSymKey(Key_Storage key_storage, String key_id) throws Exception{
    	String storage_type = key_storage.getStorage_type();
    	switch(storage_type){
    		/****** RABBIT MQ Communication: Recover the encrypted private key using RabbitMqLibrary ******/
    		case "database":
    			String db_ip = "", db_port = "", db_auth_user = "", db_auth_pwd = "", db_database = "", db_table = "";
    			ArrayList<Storage_Parameters> storage_parameters = key_storage.getStorage_parameters();
    			for(Storage_Parameters sp : storage_parameters){
    				if(sp.getName().equals("db_ip")) db_ip = sp.getValue();
    				else if(sp.getName().equals("db_port")) db_port = sp.getValue();
//    				else if(sp.getName().equals("db_auth_user")) db_auth_user = sp.getValue();
//    				else if(sp.getName().equals("db_auth_pwd")) db_auth_pwd = sp.getValue();
    				else if(sp.getName().equals("db_database")) db_database = sp.getValue();
    				else if(sp.getName().equals("db_table")) db_table = sp.getValue();
    			}
    			db_auth_user=servletConfig.get(db_ip+":"+db_port+"|username");
    			db_auth_pwd=servletConfig.get(db_ip+":"+db_port+"|password");
    			String credentials = db_auth_user + ":" + db_auth_pwd;
    			String basicAuth = "Basic " + new String(Base64.getUrlEncoder().encode(credentials.getBytes()));
    			
    			String urlGet = "http://" + db_ip + ":" + db_port + "/query/" + db_database + "/sql/" +
    							"select%20value%20from%20" + db_table + "%20where%20id=\"" + key_id + "\"";
    			URL url = new URL(urlGet);
    			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
    			connection.setRequestProperty("Authorization", basicAuth);
    			connection.setRequestMethod("GET");
    			connection.setUseCaches(false);
    			connection.setDoInput(true);
    			connection.setDoOutput(true);
    						
    			if(connection.getResponseCode()==200){
    				String json_response = "";
    				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    				String text = "";
    				while ((text = br.readLine()) != null) {
    					json_response += text;
    				}
    				Gson gson = new Gson();
    				OrientDB_Recovered_Key message_class = gson.fromJson(json_response, OrientDB_Recovered_Key.class);
    				
    				return message_class.getResult().get(0).getValue(); //There will only be one result, since the key id is unique
    			}else	
    				throw new Exception("The encrypted symmetric key has not been recovered");		
    		case "embedded":
    			ArrayList<Storage_Parameters> storage_parameters_embedded = key_storage.getStorage_parameters();
    			
    			for(Storage_Parameters sp : storage_parameters_embedded){
    				if(sp.getName().equals("encrypted_key"))
    					return sp.getValue();
    			}
    			
    		default:
    			throw new Exception("The encrypted symmetric key has not been recovered");
    	}
    }
	
/***********************************/
/********* PRIVATE METHODS *********/
/***********************************/

}
