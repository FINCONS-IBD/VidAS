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

public class ProxyLogoutUser extends ServerResource  {

	final static Logger logger = Logger.getLogger(ProxyLogoutUser.class);

	@Post
	public Representation logout(String parameters) throws ResourceException{
		
		logger.trace("Called the logout method...");
		
		logger.debug("Prepare the client for the remote call to performe the logout...");
		ClientResource clientResource = null;
		Client client = new Client(new Context(), Protocol.HTTP);
		clientResource = new ClientResource(PropertiesHelper.getProps().getProperty("getTokenServiceLogoutPath"));
		clientResource.setNext(client);

		Representation serverResponse = clientResource.post(parameters);
		
		logger.info("Logout service response returned successful ");
		
		return serverResponse;
	}


}
