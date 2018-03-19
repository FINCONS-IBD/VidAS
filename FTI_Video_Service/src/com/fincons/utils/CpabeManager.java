package com.fincons.utils;

import org.apache.log4j.Logger;

import cpabe.Cpabe;

public class CpabeManager {
	
	final static Logger logger = Logger.getLogger(CpabeManager.class);
		
	public boolean dec(String pubKey, String prvfile, String encfile, 	String decfile) throws Exception {
		
		logger.info("Called the CP ABE decryption() method");
		Cpabe cp_abe = new Cpabe();
		return cp_abe.dec(pubKey, prvfile, encfile, decfile);
	}
	
	public void enc(String pubKey, String policy, String inputfile, String encfile) throws Exception {
		
		logger.info("Called the CP ABE encryption() method");
		
		Cpabe cp_abe = new Cpabe();
		cp_abe.enc(pubKey, policy, inputfile, encfile);
	}
	
}
