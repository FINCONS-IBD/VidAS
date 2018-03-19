package com.fincons.videostorage.restlet;

import org.apache.log4j.Logger;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.LocalReference;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import com.fincons.videostorage.utils.Constants;
import com.fincons.videostorage.utils.PropertiesHelper;

import freemarker.template.Configuration;

public class VideoStorageRestletApplication extends Application {

	private Configuration configuration;
	final static Logger logger = Logger.getLogger(VideoStorageRestletApplication.class);
//	static{
//		System.getProperties().setProperty("rabbitmq_config_file", PropertiesHelper.getProps().getProperty(Constants.PATH_RABBITMQ_CONFIG));
//		System.getProperties().setProperty("log4j.configuration", PropertiesHelper.getProps().getProperty(Constants.PATH_RABBITMQ_LOG4J_CONFIG));
//	}
	@Override
	public synchronized Restlet createInboundRoot() {
		logger.info("Called the createInboundRoot restlet method...");
		
		Router router_free = new Router(getContext());
		Router router_auth = new Router(getContext());	
		
		TokenAuthenticator aut=  new TokenAuthenticator(getContext(), "My Realm");
		// Set the credentials verifier
		logger.info("Created the Restlet CustomVerifier...");
		CustomVerifier verifier = new CustomVerifier();
		aut.setVerifier(verifier);

		Configuration configuration = new Configuration();
		configuration.setTemplateLoader(new ContextTemplateLoader(getContext(), LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/templates")));
		
//		router_auth.attach("/",DirectoryResources.class);
//		router_auth.attach("/{dir}",DirectoryResources.class);
//		router_auth.attach("/{dir}/{nameFile}",VideoResources.class);
		router_auth.attach("/",NodeResources.class, Template.MODE_STARTS_WITH);
		
		router_free.attachDefault(aut);
		aut.setNext(router_auth);
		logger.info("Created and returned the Restlet Context with url-resource association...");
		
		return router_free;
	}

	public Configuration getConfiguration() {
		logger.info("Restlet Configuration loaded...");	
		return configuration;
	}

}