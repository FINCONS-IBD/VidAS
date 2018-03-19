package com.fincons.restlet;

import org.apache.log4j.Logger;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class RegistrationFailedResourceHtml extends ServerResource {

	final static Logger logger = Logger.getLogger(RegistrationFailedResourceHtml.class);
	
	@Get
	public Representation represent() throws ResourceException{

		logger.trace("Called the represent restlet RegistrationFailedResourceHtml method...");
		
		Representation mainTab = new ClientResource(LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/templates/reg_failed.html")).get();
		
		logger.info("Wraps the bean with a FreeMarker representation to show the registration falied html page...");		
		return new TemplateRepresentation(mainTab, MediaType.TEXT_HTML);
	}

}
