package com.fincons.policiesstorage.restlet;

import org.apache.log4j.Logger;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import com.fincons.policiesstorage.restlet.TokenAuthenticator;

import freemarker.template.Configuration;

public class PoliciesStorageRestletApplication extends Application {

	private Configuration configuration;
	final static Logger logger = Logger.getLogger(PoliciesStorageRestletApplication.class);

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
		
		router_auth.attach("/",Dispatcher.class, Template.MODE_STARTS_WITH);
//		router_auth.attach("/{dir}",DirectoryResources.class);
//		router_auth.attach("/{dir}/{nameFile}",PolicyResources.class);
       
		
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