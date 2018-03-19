package com.fincons.token.restlet;

import org.apache.log4j.Logger;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.LocalReference;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.routing.Router;

import com.fincons.h2.H2DatabaseEmbedded;

import freemarker.template.Configuration;

public class TokenServiceRestletApplication extends Application {

	private Configuration configuration;
	final static Logger logger = Logger.getLogger(TokenServiceRestletApplication.class);

	@Override
	public synchronized Restlet createInboundRoot() {
		logger.trace("Called the createInboundRoot restlet method...");
		
		Router router_free = new Router(getContext());
		//CREATE CONNECTION DATABASE and Create Table Users
		logger.debug("Create the H2 Database connection and the Users Table");
		H2DatabaseEmbedded.createConnection();

		router_free.attach("/checkRegistration", CheckRegistrationResource.class);
		router_free.attach("/registration", RegistrationResource.class);
		router_free.attach("/getUserData", UserDataResourceImpl.class);
		router_free.attach("/login", LoginResourceJson.class);
		router_free.attach("/generateToken", GenerateTokenResource.class);
		router_free.attach("/defPolicyToken", DefPolicyTokenResource.class);

		router_free.attach("/logout", LogoutResource.class);
		
		logger.info("Created and returned the Restlet Context with url-resource association...");
		
		return router_free;
	}

	public Configuration getConfiguration() {
		logger.info("Restlet Configuration loaded...");
		return configuration;
	}

}