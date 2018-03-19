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

public class RegistrationSuccessResourceHtml extends ServerResource {

	final static Logger logger = Logger.getLogger(RegistrationSuccessResourceHtml.class);
	
	@Get
	public Representation represent() throws ResourceException{

		logger.trace("Called the represent restlet RegistrationSuccessResourceHtml method...");
		
		Representation mainTab = new ClientResource(LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/templates/reg_success.html")).get();
		
		logger.info("Wraps the bean with a FreeMarker representation to show the registration success html page...");		
		return new TemplateRepresentation(mainTab, MediaType.TEXT_HTML);
	}

}
