package com.fincons.token.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class PropertiesHelper {
	
	final static Logger logger = Logger.getLogger(PropertiesHelper.class);
	
	private static Properties props = new Properties();

	static {

		logger.info("Loading configuration parameters...");

		InputStream input = null;

		try {
			input = PropertiesHelper.class.getClassLoader().getResourceAsStream("config/config.properties");
			props.load(input);
		} catch (Exception ex) {
			logger.error("Internal Server Error", ex);
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
	
	
	public static Properties getProps() {
		return props;
	}
	

}
