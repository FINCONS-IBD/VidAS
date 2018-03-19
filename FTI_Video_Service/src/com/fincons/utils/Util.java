package com.fincons.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;

import org.apache.log4j.Logger;

public class Util {

	final static Logger logger = Logger.getLogger(Util.class);
//	public static String user ="";
	
	public static void dropOldFile(){//String username){
		
		logger.trace("Calling the dropOldFile method...");
//		user=username;

		String path= Util.class.getClassLoader().getResource(
				PropertiesHelper.getProps().getProperty(Constants.PATH_FILE)).getPath();
		Date date = new Date();
		long limitFileTimestamp= date.getTime()-86400000;

		logger.info("Deleting the file created before " + new Date(limitFileTimestamp));
		FilenameFilter filtroCsvSoundMapp= new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
//				return name.contains(user+"-");
				return true;
			}
		};
		if(new File(path).exists()){
		 for (File f : new File(path).listFiles(filtroCsvSoundMapp)) {
			 if(f.lastModified()<=limitFileTimestamp){
				 logger.info("Deleted file "+ f.getName() +" with last modified date "+ new Date(f.lastModified()));
				 f.delete();
			 }
         }
		}else{
			 logger.warn("Directory not found");
		}
	}
}
