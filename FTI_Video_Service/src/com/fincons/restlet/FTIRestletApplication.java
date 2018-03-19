package com.fincons.restlet;

import org.apache.log4j.Logger;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.LocalReference;
import org.restlet.ext.freemarker.ContextTemplateLoader;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;

import com.fincons.restlet.RegistrationResourceHtml;
import com.fincons.restlet.proxy.AnalizedServerResource;
import com.fincons.restlet.proxy.CheckRegistrationResourceImpl;
import com.fincons.restlet.proxy.DecryptionServerResource;
import com.fincons.restlet.proxy.EncryptionServerResource;
import com.fincons.restlet.proxy.FilterResourceImpl;
import com.fincons.restlet.proxy.LoginUserImpl;
import com.fincons.restlet.proxy.ProxyGenerateTokenImpl;
import com.fincons.restlet.proxy.ProxyLogoutUser;
import com.fincons.restlet.proxy.RegistrationResourceImpl;
import com.fincons.restlet.proxy.UserDataResourceImpl;

import freemarker.template.Configuration;

public class FTIRestletApplication extends Application {

	private Configuration configuration;
	final static Logger logger = Logger.getLogger(FTIRestletApplication.class);

	@Override
	public synchronized Restlet createInboundRoot() {
		logger.trace("Called the createInboundRoot restlet method...");
		
		Router router_auth = new Router(getContext());	
		Router router_free = new Router(getContext());

		NaiveAuthenticator authenticator = new NaiveAuthenticator(getContext(), "My Realm");

		logger.trace("Created the Restlet NaiveAuthenticator...");
		
		// Set the credentials verifier
		CustomVerifier verifier = new CustomVerifier();
		authenticator.setVerifier(verifier);
		logger.info("Created the Restlet CustomVerifier...");
		
		Configuration configuration = new Configuration();
		configuration.setTemplateLoader(new ContextTemplateLoader(getContext(), LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/templates")));
		
        Directory imagesDir = new Directory(getContext(), LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/images"));
        Directory scriptsDir = new Directory(getContext(), LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/scripts"));
        Directory scriptsCryptoDir = new Directory(getContext(), LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/scripts/crypto"));
        Directory scriptBlocklyDir = new Directory(getContext(), LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/scripts/blockly/media"));
        Directory cssDir = new Directory(getContext(), LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/css"));
        Directory downloadDir = new Directory(getContext(), LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/psy_cpabe_files/fileTemp"));
        Directory messagesDir = new Directory(getContext(), LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/messages"));
        Directory framesHTML = new Directory(getContext(), LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/frames"));
        //free script and file resources
        router_free.attach("/images", imagesDir);
        router_free.attach("/css", cssDir);
        router_free.attach("/scripts", scriptsDir);
        router_free.attach("/scripts/crypto", scriptsCryptoDir);
        router_free.attach("/scripts/blockly/media", scriptBlocklyDir);
        router_free.attach("/messages", messagesDir);
        router_free.attach("/frames", framesHTML);
        
        router_free.attach("/psy_cpabe_files/fileTemp", downloadDir);
        //free service
        router_free.attach("/registration",RegistrationResourceHtml.class);
        router_free.attach("/reg_success", RegistrationSuccessResourceHtml.class);
		router_free.attach("/reg_failed", RegistrationFailedResourceHtml.class);
		router_free.attach("/proxyLogout", ProxyLogoutUser.class);
		
		//WebService component service (proxy capabilities to use Token_Service)
		router_free.attach("/proxyCheckRegistration", CheckRegistrationResourceImpl.class);
		router_free.attach("/proxyRegistration", RegistrationResourceImpl.class);
		router_free.attach("/proxyGetUserData", UserDataResourceImpl.class);
		router_free.attach("/proxyLogin", LoginUserImpl.class);
		
		//add the authenticator
		router_free.attachDefault(authenticator);
		authenticator.setNext(router_auth);

		//after the authenticator, the following resource are protected
		router_auth.attach("/document", PageResourceHtml.class);
        router_free.attach("/proxyGenerateToken", ProxyGenerateTokenImpl.class);

        router_auth.attach("/proxyFilter", FilterResourceImpl.class);
        router_auth.attach("/decryptProxy", DecryptionServerResource.class);
        router_auth.attach("/encryptProxy", EncryptionServerResource.class);
        router_auth.attach("/analizedProxy", AnalizedServerResource.class);
        logger.info("Created and returned the Restlet Context with url-resource association...");
		
		return router_free;
	}

	public Configuration getConfiguration() {
		logger.trace("Restlet Configuration loaded...");
		return configuration;
	}

}