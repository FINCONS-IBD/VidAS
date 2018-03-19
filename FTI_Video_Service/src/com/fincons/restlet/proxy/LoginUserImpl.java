package com.fincons.restlet.proxy;

import org.apache.log4j.Logger;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fincons.utils.PropertiesHelper;

public class LoginUserImpl extends ServerResource  {

	final static Logger logger = Logger.getLogger(LoginUserImpl.class);

	@Post
	public Representation login(String parameters) throws ResourceException{
		
		logger.trace("Called the login proxyCheckRegistration method...");

		logger.debug("Prepare the client for the remote call to performe the login...");
		ClientResource clientResource = null;
		Client client = new Client(new Context(), Protocol.HTTP);
		clientResource = new ClientResource(PropertiesHelper.getProps().getProperty("getTokenServiceLoginPath"));
		clientResource.setNext(client);

		Representation serverResponse = clientResource.post(parameters);
		
		logger.info("login service response returned successful ");
		
		return serverResponse;

	}


}
