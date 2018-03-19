package com.fincons.spi;

import java.util.HashMap;
import java.util.ServiceLoader;

public class ImplementationLoader {

	private static HashMap<String, DBManager> serviceImplementation = new HashMap<String, DBManager>();
	
	public static void loadServices(){
		ServiceLoader<DBManager> services = ServiceLoader.load(DBManager.class);
		for(DBManager interfaces : services){
			serviceImplementation.put(interfaces.getClass().getSimpleName(), interfaces);
		}
	}
	public static DBManager getInstance(String nameClass) throws Exception {
		if(serviceImplementation.isEmpty()){
			loadServices();
		}
		DBManager service = serviceImplementation.get(nameClass);
		if(service != null){
			return service;
		}
		else {
			throw new Exception("ERROR! IMPLEMENTATION NOT FOUND");
		}
	};
}
