package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import algorithms.Elliptic_Curve_Diffie_Hellman;
import messages.EPK_Info;
import messages.JSON_Web_Key;

/*
 * @author Salvador P�rez
 * @version 29/11/2016
 */

/**
 * Servlet implementation class Generate_Shared_Secret
 */
public class Generate_Shared_Secret extends HttpServlet {
	private static final long serialVersionUID = 1L;
//	private long time_dh;

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		/* Read parameters from the configure file */
		/* Read parameters from the configure file */
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{

			/*  TEST 1: CONSUMED MEMORY AND TIME: ABE PROXY */
//	        Runtime runtime = Runtime.getRuntime();
//			runtime.gc();
//			long startTime = System.currentTimeMillis();
//			this.time_dh = 0;
			
			BufferedReader in = new BufferedReader(request.getReader());
			String line;
			String payload = "";
	        while ((line = in.readLine()) != null) {
	        	payload += line;
	        	System.out.println(line);
	        }
	        
/* 1.3. Generate an ephemeral elliptic curve key pair */
	        Gson gson = new Gson();
	        EPK_Info device_epk_info = gson.fromJson(payload, EPK_Info.class);
	        
//	        long startTimeDH = System.currentTimeMillis();
	        
	        Elliptic_Curve_Diffie_Hellman ecdh = new Elliptic_Curve_Diffie_Hellman(device_epk_info.getJwk().getKty(), device_epk_info.getJwk().getCrv());
	        
//	        long endTimeDH   = System.currentTimeMillis();
//			this.time_dh += endTimeDH - startTimeDH;
	        
/* 1.3. Generate an ephemeral elliptic curve key pair */
	          
/* 1.4. Generate the JSON with its ephemeral public key */
	        /* Get JSON "proxy_epk_info" */
			JSON_Web_Key jwk = new JSON_Web_Key(ecdh.getKey_type(), ecdh.getCryptographic_curve(), ecdh.getXparameterB64url(), ecdh.getYparameterB64url());
			String alg = device_epk_info.getAlg();
			String enc = device_epk_info.getEnc();
			String apu = device_epk_info.getApu();
			String apv = device_epk_info.getApv();
			EPK_Info proxy_epk_info_class = new EPK_Info(alg, enc, apu, apv, jwk);
			String proxy_epk_info = gson.toJson(proxy_epk_info_class);
/* 1.4. Generate the JSON with its ephemeral public key */
			
//			startTimeDH = System.currentTimeMillis();
			
/* 1.5. Calculate the shared secret by Diffie-Hellman algorithm */		
			ecdh.ECDH_ES(device_epk_info);
/* 1.5. Calculate the shared secret by Diffie-Hellman algorithm */
			
/* 1.6. Calculate the symmetric shared key by Concat KDF algorithm */		
			ecdh.Concat_KDF(device_epk_info);
/* 1.6. Calculate the symmetric shared key by Concat KDF algorithm */
			
//			endTimeDH   = System.currentTimeMillis();
//			this.time_dh += endTimeDH - startTimeDH;
			
/* 1.7. Delete ephemeral key pair */		
			ecdh.Delete_ekeys();
/* 1.7. Delete ephemeral key pair */
			
/* 1.8. Send its ephemeral public key to smart device */
			/* Store the ecdh object for the this smart device --> IMPORTANT!! The smart device ID must be unique (the "apu" attribute) */
			String smart_device_id = new String(Base64.getUrlDecoder().decode(apu));
			request.getServletContext().setAttribute(smart_device_id, ecdh);
			
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentLength(proxy_epk_info.length());
			OutputStream os = response.getOutputStream();
			os.write(proxy_epk_info.getBytes());
			os.close();
/* 1.8. Send its ephemeral public key to smart device */
			
			/*  TEST 1: CONSUMED MEMORY AND TIME: ABE PROXY */
//			long endTime   = System.currentTimeMillis();
//			runtime = Runtime.getRuntime();
//			runtime.gc();
//			long memory = runtime.totalMemory() - runtime.freeMemory();
//			long totalTime = endTime - startTime;
//			try {
//				File TextFile = new File("mem_pub_gen_proxy.csv"); 
//				FileWriter TextOut = new FileWriter(TextFile, true);
//				TextOut.write(memory/1024L + "\n");
//				TextOut.close();
//				TextFile = new File("time_pub_gen_proxy.csv"); 
//				TextOut = new FileWriter(TextFile, true);
//				TextOut.write(totalTime + ";");
//				TextOut.write(this.time_dh + "\n");
//				TextOut.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}			
			
			System.out.println("SHARED SYMMETRIC KEY: " + Base64.getUrlEncoder().withoutPadding().encodeToString(ecdh.getShared_Sym_Key()));
		
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);			
		}
	}

/***********************************/
/********* PRIVATE METHODS *********/
/***********************************/

	
/***********************************/
/********* PRIVATE METHODS *********/
/***********************************/

}
