package com.fincons.keygenerator.restlet;

import org.apache.log4j.Logger;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import freemarker.template.Configuration;

public class KeyGeneratorRestletApplication extends Application {

	private Configuration configuration;
	final static Logger logger = Logger.getLogger(KeyGeneratorRestletApplication.class);

	@Override
	public synchronized Restlet createInboundRoot() {
		logger.info("Called the createInboundRoot restlet method...");
		
		Router router_auth = new Router(getContext());	
		Router router_free = new Router(getContext());

		TokenAuthenticator authenticator=  new TokenAuthenticator(getContext(), "My Realm");
		
		logger.info("Created the Restlet TokenAuthenticator...");
		
		// Set the credentials verifier
		CustomVerifier verifier = new CustomVerifier();
		authenticator.setVerifier(verifier);
		logger.info("Created the Restlet CustomVerifier...");
		
		router_free.attach("/publicKey", PublicKeyResource.class);
		router_free.attachDefault(authenticator);
		
		authenticator.setNext(router_auth);
        router_auth.attach("/keygen",KeyGeneratorResource.class);
        
		logger.info("Created and returned the Restlet Context with url-resource association...");
		
		return router_free;
	}

	public Configuration getConfiguration() {
		logger.info("Restlet Configuration loaded...");
		return configuration;
	}

}