package com.fincons.restlet;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.restlet.data.LocalReference;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.ext.freemarker.TemplateRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fincons.restlet.ldap.LdapManager;
import com.fincons.utils.Constants;
import com.fincons.utils.PropertiesHelper;

public class PageResourceHtml extends ServerResource {

	final static Logger logger = Logger.getLogger(PageResourceHtml.class);
	@Get
	public Representation represent() throws ResourceException{

		logger.trace("Called the represent restlet PageResourceHtml method...");
		
		Map<String, Object> dataModel = new HashMap<String, Object>();
		String username=getRequest().getClientInfo().getUser().getIdentifier();

		dataModel.put(Constants.USERNAME, username);
		dataModel.put("proxyGenerateTokenServicePath", PropertiesHelper.getProps().getProperty("proxyGenerateTokenServicePath"));		
		dataModel.put("proxyLogoutServicePath", PropertiesHelper.getProps().getProperty("proxyLogoutServicePath"));
		dataModel.put("proxyFilterServicePath", PropertiesHelper.getProps().getProperty("proxyFilterServicePath"));
		try {
			dataModel.put("attributes", LdapManager.getAttributes());
		} catch (NamingException ne) {	
			logger.error("Error in Ldap", ne);
		}
		dataModel.put("analysisServices", Arrays.asList(PropertiesHelper.getProps().getProperty("AnalysisServices").split("\\s*,\\s*")));
		
		dataModel.put("analyzedService", PropertiesHelper.getProps().getProperty("analyzedService"));
		dataModel.put("policyService", PropertiesHelper.getProps().getProperty("policyService"));
		dataModel.put("videoService", PropertiesHelper.getProps().getProperty("videoService"));
		dataModel.put("keygenService", PropertiesHelper.getProps().getProperty("keygenService"));
		
		String abe_Proxy= PropertiesHelper.getProps().get(Constants.ABE_PROXY_PROTOCOL)+"://"+ PropertiesHelper.getProps().get(Constants.ABE_PROXY_IP)+":"+PropertiesHelper.getProps().get(Constants.ABE_PROXY_PORT)+"/"+
				PropertiesHelper.getProps().get(Constants.ABE_PROXY_ID);
		dataModel.put("abe_Proxy",abe_Proxy);
		dataModel.put("key_alg", PropertiesHelper.getProps().get(Constants.KEY_ALG));
		dataModel.put("key_crv", PropertiesHelper.getProps().get(Constants.KEY_CRV));
		dataModel.put("key_enc", PropertiesHelper.getProps().get(Constants.KEY_ENC));
		dataModel.put("key_kty", PropertiesHelper.getProps().get(Constants.KEY_KTY));
		
		dataModel.put("key_storage_type", PropertiesHelper.getProps().get(Constants.KEY_STORAGE_ID));
		dataModel.put("key_storage_ip", PropertiesHelper.getProps().get(Constants.KEY_STORAGE_IP));
		dataModel.put("key_storage_port", PropertiesHelper.getProps().get(Constants.KEY_STORAGE_PORT));
		dataModel.put("key_db_database", PropertiesHelper.getProps().get(Constants.KEY_STORAGE_DB_DATABASE));
		dataModel.put("key_db_table", PropertiesHelper.getProps().get(Constants.KEY_STORAGE_DB_TABLE));
		
		Representation mainTab = new ClientResource(LocalReference.createClapReference(LocalReference.CLAP_CLASS, "/templates/mainTab_new.html")).get();
		
		logger.info("Wraps the bean with a FreeMarker representation to show the main page html page, after login success...");		
		return new TemplateRepresentation(mainTab, dataModel, MediaType.TEXT_HTML);
	}

}
