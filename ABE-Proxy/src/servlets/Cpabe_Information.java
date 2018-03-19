package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

import algorithms.AES;
import algorithms.CPABE;
import algorithms.Elliptic_Curve_Diffie_Hellman;
import algorithms.HMAC;
import cpabe.Common;
import messages.CPABE_Info;
import messages.Enc_CPABE_Info;
import messages.Enc_CPABE_InfoSign;
import messages.Key_Id;
import messages.Key_Storage;
import messages.Metadata;
import messages.OrientDB_Saved_Key;
import messages.Storage_Parameters;

/*
 * @author Salvador P�rez
 * @version 29/11/2016
 */

/**
 * Servlet implementation class Cpabe_Information
 */
public class Cpabe_Information extends HttpServlet {
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
	 */
	public void init(ServletConfig config) throws ServletException {
		try {
			/* Read parameters from the configure file */
			//ServletConfig conf=getServletConfig();
			//this.kgs_ip = config.getInitParameter("kgs_ip");
			//this.kgs_port = config.getInitParameter("kgs_port");
			
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
//			this.time_aes = 0;
//			this.time_cpabe = 0;
			
			this.id = config.getServletContext().getInitParameter("proxy_id");
		
			Enumeration<String> configParams=config.getServletContext().getInitParameterNames();
//			String param =configParams.nextElement();
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

			/*  TEST 1: CONSUMED MEMORY AND TIME: ABE PROXY */
//	        Runtime runtime = Runtime.getRuntime();
//			runtime.gc();
//			long startTime = System.currentTimeMillis();
//			this.time_aes = 0;
//			this.time_cpabe = 0;
			
			TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
			
			BufferedReader in = new BufferedReader(request.getReader());
			String line;
			String payload = "";
	        while ((line = in.readLine()) != null) {
	        	payload += line;
//	        	System.out.println(line);
	        }
	        
/* 2.3. Decrypt CP-ABE related information by AES (shared_sym_key) */
	        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
//	        Gson gson = new Gson();
//	        Enc_CPABE_Info enc_cpabe_info = gson.fromJson(payload, Enc_CPABE_Info.class);
	        Enc_CPABE_InfoSign enc_cpabe_info = gson.fromJson(payload, Enc_CPABE_InfoSign.class);
		   
	        
	        /* Recovery the symmetric key associated with the smart device in this context */
	        String smart_device_id = "";
	        for(Metadata m : enc_cpabe_info.getMetadata()){
	        	if(m.getName().equals("encryptor"))
	        		smart_device_id = m.getValue();
	        }
	        if(smart_device_id.equals(""))
	        	throw new Exception("The \"encryptor\" metadata is not specified");
	        Elliptic_Curve_Diffie_Hellman ecdh = (Elliptic_Curve_Diffie_Hellman)request.getServletContext().getAttribute(smart_device_id);
	        if(ecdh == null)
	        	throw new Exception("The symmetric key associated with the smart device " + smart_device_id + " has not been established (PHASE 1)");
	        /* Recovery the symmetric key associated with the smart device in this context */
	        
//	        System.out.println(enc_cpabe_info.getEnc_cpabe_info());
	        byte[] enc_cpabe_info_byte = Base64.getUrlDecoder().decode(enc_cpabe_info.getEnc_cpabe_info().getEnc_cpabe());
//	        System.out.println("file"+Arrays.toString(enc_cpabe_info_byte));
	        byte[] shared_sym_key = ecdh.getShared_Sym_Key();
	   
//	        long startTimeAES = System.currentTimeMillis();
	        byte[] key2= HMAC.generateHMACKeySha256(shared_sym_key);
	        String enc_cpabe_info_toSign_json=gson.toJson(enc_cpabe_info.getEnc_cpabe_info());
	        byte[] hmac=HMAC.signHMACSHA256(enc_cpabe_info_toSign_json.getBytes(), key2);
	       
	        String hmacB64=Base64.getUrlEncoder().withoutPadding().encodeToString(hmac);
			if(!hmacB64.equals(enc_cpabe_info.getTag())){
	        	throw new Exception("Signature not verified");
			}
	        
	        byte[] iv = Base64.getUrlDecoder().decode(enc_cpabe_info.getEnc_cpabe_info().getIv());
		        
	        AES aes = new AES();
			byte[] cpabe_info_byte = aes.AES_decrypt(enc_cpabe_info_byte, shared_sym_key, iv);
			
//			long endTimeAES   = System.currentTimeMillis();
//			this.time_aes += endTimeAES - startTimeAES;
			
			String cpabe_info_string = new String(cpabe_info_byte);
			CPABE_Info cpabe_info = gson.fromJson(cpabe_info_string, CPABE_Info.class);
			System.out.println(cpabe_info_string);
			/* Delete the symmetric key associated with the smart device in this context and all information about such symmetric key */
			request.getServletContext().removeAttribute(smart_device_id);
			
			/* Delete the symmetric key associated with the smart device in this context and all information about such symmetric key */
/* 2.3. Decrypt CP-ABE related information by AES (shared_sym_key) */
        
/* 2.6. Encrypt shared_sym_key with CP-ABE */
	        String policy = cpabe_info.getPolicy().getSpecs();
	        
//	        long startTimeCPABE = System.currentTimeMillis();
	        
	        CPABE cpabe = new CPABE();
	        byte[][] enc_sym_key = cpabe.CPABE_encrypt(public_parameters, policy, shared_sym_key);
	        
//	        long endTimeCPABE   = System.currentTimeMillis();
//			this.time_cpabe += endTimeCPABE - startTimeCPABE;
	        
	        if(enc_sym_key==null)
	        	throw new Exception("Error encrypting symmetric key");
/* 2.6. Encrypt shared_sym_key with CP-ABE */	        

/* 2.7. Store enc_sym_key in a Key Store Service */
	        /* Generate a unique key identifier associated with the encrypted symmetric key */
	        String enc_sym_key_id_cp = UUID.randomUUID().toString();
	        String enc_sym_key_id = enc_sym_key_id_cp.replaceAll("-", "_");
	        /* At the moment, the symmetric key is only saved in the first storage */
			Key_Storage key_storage = cpabe_info.getEncrypted_symmetric_key_storage();
	        
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
			Date date = new Date();
			String encryption_date = df.format(date);
			Metadata encryption_date_metadata = new Metadata("encryption-date", encryption_date);
			
			String encryptor = this.id;
			Metadata encryptor_metadata = new Metadata("encryptor", encryptor);
			
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());
			calendar.add(Calendar.SECOND, 5); //Key expiration date
			String expiration_date = df.format(calendar.getTime());
			Metadata expiration_date_metadata = new Metadata("expiration-date", expiration_date);
			
			String encryption_algorithms = "CP-ABE(AES)";
			Metadata encryption_algorithms_metadata = new Metadata("protection-mechanism", encryption_algorithms);
			
	        /* Obtain the first key storage service to save the encrypted symmetric key along with its metadatas and its identifier*/
	        String cph_b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(enc_sym_key[0]);
	        String aes_b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(enc_sym_key[1]);
	        String enc_sym_key_string = cph_b64 + " " + aes_b64;
	        
//	        System.out.println("Cpabe_info enc_sym_key_string"+enc_sym_key_string);
//			 
//		    System.out.println("Array Byte cph_b enc_sym_key_string"+Arrays.toString(enc_sym_key[0]));
//		    System.out.println("Array Byte aes_b enc_sym_key_string"+Arrays.toString(enc_sym_key[1]));
	       
		    
	        
	        this.saveEncSymKey(key_storage, enc_sym_key_id, enc_sym_key_string, encryption_date, encryption_algorithms, encryptor, expiration_date);
/* 2.7. Store enc_sym_key in a Key Store Service */

/* 2.9. Send the enc_sym_key_id to the smart device */
			ArrayList<Metadata> key_id_metadatas = new ArrayList<Metadata>();
			key_id_metadatas.add(encryption_date_metadata);key_id_metadatas.add(encryption_algorithms_metadata);key_id_metadatas.add(encryptor_metadata);key_id_metadatas.add(expiration_date_metadata);
			Key_Id key_id = new Key_Id(enc_sym_key_id, key_storage, key_id_metadatas);
	        
	        String key_id_string = gson.toJson(key_id);
	        response.setStatus(HttpServletResponse.SC_OK);
			response.setContentLength(key_id_string.length());
			OutputStream os = response.getOutputStream();
			os.write(key_id_string.getBytes());
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
	
	private void saveEncSymKey(Key_Storage key_storage, String enc_sym_key_id, String enc_sym_key_string, 
							   String encryption_date, String encryption_algorithms, String encryptor, String expiration_date) throws Exception{
		String storage_type = key_storage.getStorage_type();
    	switch(storage_type){
    		/****** RABBIT MQ Communication: Store the encrypted private key using RabbitMqLibrary ******/
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
    			
    			//get user and password from ip:port from config
    			db_auth_user=servletConfig.get(db_ip+":"+db_port+"|username");
    			db_auth_pwd=servletConfig.get(db_ip+":"+db_port+"|password");
    			
    			String credentials = db_auth_user + ":" + db_auth_pwd;
    			String basicAuth = "Basic " + new String(Base64.getUrlEncoder().encode(credentials.getBytes()));
    			
    			String urlPost = "http://" + db_ip + ":" + db_port + "/document/" + db_database;
    			URL url = new URL(urlPost);
    			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
    			connection.setRequestProperty("Authorization", basicAuth);
    			connection.setRequestMethod("POST");
    			connection.setRequestProperty("Content-type", "application/json");
    			connection.setUseCaches(false);
    			connection.setDoInput(true);
    			connection.setDoOutput(true);
    			/* Payload */
    			OrientDB_Saved_Key message_class = new OrientDB_Saved_Key(db_table, enc_sym_key_id, enc_sym_key_string, encryption_date, encryption_algorithms, encryptor, expiration_date);
    			Gson gson = new Gson();
    			String message_json = gson.toJson(message_class);
//    			System.out.println("JSON: " + message_json);
    			OutputStream os = connection.getOutputStream();
    			os.write(message_json.getBytes("UTF-8"));
    			os.close();
    		    			
    			if(connection.getResponseCode()!=201)
    				throw new Exception("The encrypted symmetric key has not been saved");			
 
    			break;
    		case "embedded":
    			ArrayList<Storage_Parameters> new_storage_parameters = key_storage.getStorage_parameters();
    			if(new_storage_parameters==null){
    				new_storage_parameters= new ArrayList<Storage_Parameters>();
    			}
    			new_storage_parameters.add(new Storage_Parameters("encrypted_key", enc_sym_key_string));
    			new_storage_parameters.add(new Storage_Parameters("encryptor", encryptor));
    			break;
    		default:
    			throw new Exception("The encrypted symmetric key has not been saved");
    	}
	}

	
/***********************************/
/********* PRIVATE METHODS *********/
/***********************************/

}
