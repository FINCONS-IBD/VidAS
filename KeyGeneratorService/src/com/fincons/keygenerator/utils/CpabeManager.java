package com.fincons.keygenerator.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

import cpabe.Cpabe;

public class CpabeManager {
	
	final static Logger logger = Logger.getLogger(CpabeManager.class);
	
	static {
		
		logger.info("Loading configuration parameters and setup CP ABE Context...");

		InputStream input = null;

		try {
			
			String key_dir_name = PropertiesHelper.getProps().getProperty(Constants.CPABE_KEY_PATH_NAME);

			String public_key_name = PropertiesHelper.getProps().getProperty(Constants.CPABE_PUBLIC_KEY_NAME);
			String master_key_name = PropertiesHelper.getProps().getProperty(Constants.CPABE_MASTER_KEY_NAME);

//			String key_path = CpabeManager.class.getClassLoader().getResource(key_dir_name).getPath();
			
//			File f_p = new File(key_path+public_key_name);
			
//			File f_m = new File(key_path+master_key_name);
			File f_p = new File(key_dir_name+public_key_name);
			
			File f_m = new File(key_dir_name+master_key_name);
			
			if(f_p.exists() && f_m.exists()){
				logger.info("CP ABE context with public and master key found. Setup skipped...");
			}
			else{
//				setup(key_path+public_key_name, key_path+master_key_name);
				setup(key_dir_name+public_key_name, key_dir_name+master_key_name);
			}

		} catch (Exception ex) {
			logger.error("Exception", ex);
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void setup(String pubfile, String mskfile) throws IOException,
	ClassNotFoundException {
		
		logger.info("Called the CP ABE setup() method");
		
		Cpabe cp_abe = new Cpabe();
		cp_abe.setup(pubfile, mskfile);
	}
	
	public static String keygen(String attr_str) throws NoSuchAlgorithmException, IOException {
		
		logger.info("Called the CP ABE keygen() method");
		
		String key_dir_name = PropertiesHelper.getProps().getProperty(Constants.CPABE_KEY_PATH_NAME);
//		String key_path = CpabeManager.class.getClassLoader().getResource(key_dir_name).getPath();

		String public_key_name = PropertiesHelper.getProps().getProperty(Constants.CPABE_PUBLIC_KEY_NAME);
		String master_key_name = PropertiesHelper.getProps().getProperty(Constants.CPABE_MASTER_KEY_NAME);
		
		Cpabe cp_abe = new Cpabe();
//		String json_keys = cp_abe.keygen(key_path+public_key_name, key_path+master_key_name, attr_str);
		String json_keys = cp_abe.keygen(key_dir_name+public_key_name, key_dir_name+master_key_name, attr_str);
		return json_keys;
	}
	
}
