package com.fincons.token.restlet.ldap;

import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.fincons.token.utils.Constants;
import com.fincons.token.utils.PropertiesHelper;

public class LdapManager {

	final static Logger logger = Logger.getLogger(LdapManager.class);

	private static DirContext context = null;

	public static void init() throws AuthenticationNotSupportedException, AuthenticationException, NamingException {

		logger.trace("Called the init() method...");

		String urlServer = PropertiesHelper.getProps().getProperty(Constants.LDAP_SERVER);
		String user = PropertiesHelper.getProps().getProperty(Constants.LDAP_USER);
		String psw = PropertiesHelper.getProps().getProperty(Constants.LDAP_PSW);

		logger.debug("LDAP Connection params: " + urlServer + ", " + user);

		Hashtable env = new Hashtable();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, urlServer);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, user);
		env.put(Context.SECURITY_CREDENTIALS, psw);
		logger.debug("Initialize the DS Contex...");
		context = new InitialDirContext(env);
		// readTree();
		logger.error("Connected to the context: " + context.getEnvironment());

	}

	public void closeContext() {
		logger.trace("Called the closeContext() method");

		if (context != null) {
			try {
				context.close();
			} catch (NamingException e) {
				logger.error("NamingException", e);
				e.printStackTrace();
			}
		}
	}

	public static boolean setCertificate(String username, JSONObject json)
			throws AuthenticationNotSupportedException, AuthenticationException, NamingException {
		logger.info("Called the addEntry() method: " + json);

		if (context == null) {
			init();
		}
		boolean flag = false;
		String pathLdap = "";
		try {
			NamingEnumeration results = find("(mail=" + username + ")");
			while (results.hasMore()) {
				SearchResult sr2 = (SearchResult) results.next();
				pathLdap = sr2.getNameInNamespace();
			}
			ModificationItem[] mods = new ModificationItem[1];

			// Replace the "mail" attribute with a new value
			mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
					new BasicAttribute("fincsecuritycertificate", json.toString().replaceAll("\"", "\'")));
			context.modifyAttributes(pathLdap, mods);

			logger.info("Modify Entity succesfull OK.");

			flag = true;
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
			return flag;
		}
		return flag;

	}

	private static NamingEnumeration find(String filter)
			throws AuthenticationNotSupportedException, AuthenticationException, NamingException {

		logger.info("Called the find() method");

		if (context == null) {
			init();
		}
		try {
			String searchDn = PropertiesHelper.getProps().getProperty(Constants.LDAP_ROOT);
			SearchControls searchControls = new SearchControls();
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			NamingEnumeration results2 = context.search(searchDn, filter, searchControls);
			System.out.println(filter);
			return results2;
		} catch (NamingException e) {
			logger.error("Error during filter searching " + filter, e);
			e.printStackTrace();
			return null;
		}

	}

	public static JSONObject searchUser(JSONObject json) throws NamingException {
		logger.info("Called the find() searchUser: " + json);

		if (context == null) {
			init();
		}
		JSONObject result = new JSONObject();
		try {
			NamingEnumeration results = find("(mail=" + json.getString("mail") + ")");
			boolean flag = false;
			while (results.hasMore()) {
				flag = true;
				SearchResult sr2 = (SearchResult) results.next();
				String fullDistinguishedName = sr2.getName();

				Attributes attrs = sr2.getAttributes();
				result.put("message", "OK");
				result.put("code", 200);

				result.put("sn", attrs.get("sn").get());
				result.put("mail", attrs.get("mail").get());
				result.put("role", attrs.get("vidasrole").get());
				String[] parts = fullDistinguishedName.split(",");
				String part_i = "", part_i_key = "", part_i_value = "";

				for (int i = 0; i < parts.length; i++) {
					part_i = parts[i];
					part_i_key = part_i.split("=")[0];
					part_i_value = part_i.split("=")[1];
					result.put(part_i_key, part_i_value.replace(' ', '_'));
					// N.B. sostituisce lo spazio presente nel valore di un
					// campo, con un underscore,
					// per evitare problemi nel confronto policy-attributi
					// utente
					// attributi_utente+=" "+part_i_key+
					// ":"+part_i_value.replace(' ', '_');
				}

				result.put("certificate", attrs.get("fincsecuritycertificate").get());
			}
			if (!flag) {
				result.put("message", "user not found");
				System.out.println("NOT FOUND");
			}
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
		}

		return result;
	}

	public static String getDefaultPolicy(String user) {
		String policyPath = "";
		try {
			boolean flag = false;
			NamingEnumeration results = find("(mail=" + user + ")");
			while (results.hasMore()) {
				flag = true;
				SearchResult sr2 = (SearchResult) results.next();

				Attributes attrs = sr2.getAttributes();
				policyPath = (String) attrs.get("vidasuserdefaultpolicy").get();
			}
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
		}
		return policyPath;
	}
}
