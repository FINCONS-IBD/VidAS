package com.fincons.restlet;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fincons.utils.PropertiesHelper;

public class RegistrationResourceHtml extends ServerResource {
	
	final static Logger logger = Logger.getLogger(RegistrationResourceHtml.class);

	@Get
	public Representation represent() throws ResourceException{
		
		logger.trace("Called the represent restlet RegistrationResourceHtml method...");
		
		Map<String, Object> dataModel = new HashMap<String, Object>();
		dataModel.put("proxyCheckRegistrationServicePath", PropertiesHelper.getProps().getProperty("proxyCheckRegistrationServicePath"));
		dataModel.put("proxyRegistrationServicePath", PropertiesHelper.getProps().getProperty("proxyRegistrationServicePath"));
//		dataModel.put("listOrganization", LdapManager.getListOrganization());

		Representation mainTab = new ClientResource(LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/templates/registration.html")).get();
		
		logger.info("Wraps the bean with a FreeMarker representation to show the registration html page...");		
		return new TemplateRepresentation(mainTab, dataModel, MediaType.TEXT_HTML);
	}

}
