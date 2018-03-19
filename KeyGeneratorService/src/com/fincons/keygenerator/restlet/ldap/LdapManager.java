package com.fincons.keygenerator.restlet.ldap;

import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.fincons.keygenerator.utils.Constants;
import com.fincons.keygenerator.utils.PropertiesHelper;

public class LdapManager {

	final static Logger logger = Logger.getLogger(LdapManager.class);

	private static DirContext context=null; 

	public static void init() throws NamingException {

		logger.info("Called the init() method");
		
		String urlServer = PropertiesHelper.getProps().getProperty(Constants.LDAP_SERVER);
		String user = PropertiesHelper.getProps().getProperty(Constants.LDAP_USER);
		String psw = PropertiesHelper.getProps().getProperty(Constants.LDAP_PSW);


		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, urlServer);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, user);
		env.put(Context.SECURITY_CREDENTIALS, psw);
//		try {
			context= new InitialDirContext(env);
			logger.error("Connected to the context: " + context.getEnvironment());

//		} catch (AuthenticationNotSupportedException ex) {
//			logger.error("AuthenticationNotSupportedException", ex);
//			ex.printStackTrace();
//		} catch (AuthenticationException ex) {
//			logger.error("AuthenticationException", ex);
//			ex.printStackTrace();
//		} catch (NamingException ex) {
//			logger.error("NamingException", ex);
//			ex.printStackTrace();
//		}

	}

	public void closeContext()
	{
		logger.info("Called the closeContext() method");
		
		if (context!=null){
			try {
				context.close();
			} catch (NamingException e) {
				logger.error("NamingException", e);
				e.printStackTrace();
			}
		}
	}

	private static NamingEnumeration find(String filter){
		
		logger.info("Called the find() method");
		
	
		try {
			if (context==null){
				init();
			}
			String searchDn = PropertiesHelper.getProps().getProperty(Constants.LDAP_ROOT);
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration results2= context.search(searchDn ,filter, searchControls);
			System.out.println(filter);
			return results2;
		} catch (NamingException e) {
			logger.error("Error during filter searching " +filter, e);
			e.printStackTrace();
			return null;
		}

	}

	public static JSONObject searchUser(JSONObject json) throws NamingException{
		logger.info("Called the find() searchUser: "+json);

		if (context==null){
			init();
		}
		JSONObject result= new JSONObject();
		try
		{
			NamingEnumeration results = find("(mail=" + json.getString("mail")+ ")");
			boolean flag = false;
			while (results.hasMore()) {
				flag = true;
				SearchResult sr2 = (SearchResult) results.next();
				String fullDistinguishedName = sr2.getName();

				Attributes attrs = sr2.getAttributes();
				result.put("message", "OK");
				result.put("code", 200);

				JSONObject user_attributes = new JSONObject();

				String attributi_utente = "";				

				//N.B. l'ho commentato perchè già presente nel full distinguished name
				//				attributi_utente+="cn:" + attrs.get("cn").get();
				//				attributi_utente+=" sn:" + ((String)attrs.get("sn").get()).replace(' ', '_');

				attributi_utente+="sn:" + ((String)attrs.get("sn").get()).replace(' ', '_');
				attributi_utente+=" mail:" + attrs.get("mail").get();
				attributi_utente+=" vidasrole:"+ attrs.get("vidasrole").get();
				
				String[] parts = fullDistinguishedName.split(",");
				String part_i = "", part_i_key = "", part_i_value = "";

				for(int i = 0; i < parts.length; i++){
					part_i = parts[i];
					part_i_key = part_i.split("=")[0];
					part_i_value = part_i.split("=")[1];

					//N.B. sostituisce lo spazio presente nel valore di un campo, con un underscore, 
					//per evitare problemi nel confronto policy-attributi utente
					attributi_utente+=" "+part_i_key+ ":"+part_i_value.replace(' ', '_');
				}

				result.put("user_attributes", attributi_utente);

//				result.put("certificate", attrs.get("fincsecuritycertificate").get());
			}
			if (!flag) {
				result.put("message", "user not found");
				result.put("code", 404);
				System.out.println("NOT FOUND");
			}
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
		}

		return result;
	}
	
}
