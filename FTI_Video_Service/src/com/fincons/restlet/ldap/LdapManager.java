package com.fincons.restlet.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.fincons.utils.BlocklyAttribute;
import com.fincons.utils.Constants;
import com.fincons.utils.PropertiesHelper;

public class LdapManager {

	final static Logger logger = Logger.getLogger(LdapManager.class);

	private static DirContext context=null; 

	
	public static void init() throws NamingException{

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
		
		context= new InitialDirContext(env);
		logger.info("Connected to the context: " + context.getEnvironment());

		
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

//	public static boolean addEntry(JSONObject json)
//	{
//		logger.info("Called the addEntry() method: " + json);
//		
//		if (context==null){
//			init();
//		}
//		boolean flag = false;
//		Attribute userCn = new BasicAttribute("cn",json.opt("cn"));
//		Attribute userSn = new BasicAttribute("sn", json.opt("sn"));
//		String description_string = json.opt("description").toString();
//		Attribute description = new BasicAttribute("description", description_string.replaceAll("\"", "'"));
//		Attribute mail = new BasicAttribute("mail", json.opt("mail"));
//
//		//ObjectClass attributes
//		Attribute oc = new BasicAttribute("objectClass");
//		String obj_classes = PropertiesHelper.getProps().getProperty(Constants.LDAP_OBJECT_CLASSES);
//		String[] classes_vector = obj_classes.split(";");
//		for(int i = 0; i<classes_vector.length;i++){
//			oc.add(classes_vector[i]);
//		}
//
//		Attributes entry = new BasicAttributes();
//		entry.put(userCn);
//		entry.put(userSn);
//		entry.put(description);
//		entry.put(mail);
//		entry.put(oc);
//		try{
//
//			String country=findOrganizationCountry(json.optString("o"));
//			String entryDN = "cn="+userCn.get()+"," + PropertiesHelper.getProps().getProperty(json.optString("o"))+",countryName="+country+",dc=example,dc=com";
//			System.out.println("entryDN: " + entryDN);
//
//			context.createSubcontext(entryDN, entry);
//			
//			logger.info("New Entity succesfull created.");
//			
//			flag = true;
//		}catch(Exception e){
//			logger.error("Exception", e);
//			e.printStackTrace();
//			return flag;
//		}
//		return flag	;
//
//	}
//
//	public static String findOrganizationCountry(String organization){
//		
//		logger.info("Called the findOrganizationCountry() method");
//		
//		String country="";
//		NamingEnumeration  result = find("(o="+organization+")");
//		try{
//
//			while(result.hasMore())
//			{
//				SearchResult sr2 = (SearchResult) result.next();
//				String namespace= sr2.getName();
//				String[] nodes= namespace.split(",");
//				for (String string : nodes) {
//					if(string.contains("countryName")){
//						country=string.split("=")[1];
//					}
//				}
//			}
//		}catch(NamingException e) {
//			logger.error("Error searching Country associated to " +organization, e);
//			e.printStackTrace();
//		}
//		return country;
//	}

	private static NamingEnumeration find(String filter) {
		
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

//	public static JSONObject searchUser(JSONObject json){
//		logger.info("Called the find() searchUser: "+json);
//
//		if (context==null){
//			init();
//		}
//		JSONObject result= new JSONObject();
//		try
//		{
//			NamingEnumeration results = find("(mail=" + json.getString("mail")+ ")");
//			boolean flag = false;
//			while (results.hasMore()) {
//				flag = true;
//				SearchResult sr2 = (SearchResult) results.next();
//				String fullDistinguishedName = sr2.getName();
//
//				Attributes attrs = sr2.getAttributes();
//				result.put("message", "OK");
//				result.put("code", 200);
//
//				JSONObject user_attributes = new JSONObject();
//
//				String attributi_utente = "";				
//
//				//N.B. l'ho commentato perchè già presente nel full distinguished name
//				//				attributi_utente+="cn:" + attrs.get("cn").get();
//				//				attributi_utente+=" sn:" + ((String)attrs.get("sn").get()).replace(' ', '_');
//
//				attributi_utente+="sn:" + ((String)attrs.get("sn").get()).replace(' ', '_');
//				attributi_utente+=" mail:" + attrs.get("mail").get();
//
//				String[] parts = fullDistinguishedName.split(",");
//				String part_i = "", part_i_key = "", part_i_value = "";
//
//				for(int i = 0; i < parts.length; i++){
//					part_i = parts[i];
//					part_i_key = part_i.split("=")[0];
//					part_i_value = part_i.split("=")[1];
//
//					//N.B. sostituisce lo spazio presente nel valore di un campo, con un underscore, 
//					//per evitare problemi nel confronto policy-attributi utente
//					attributi_utente+=" "+part_i_key+ ":"+part_i_value.replace(' ', '_');
//				}
//
//				result.put("user_attributes", attributi_utente);
//				
////				result.put("role", attrs.get("vidasrole").get());
//				result.put("certificate", attrs.get("fincsecuritycertificate").get());
//			}
//			if (!flag) {
//				result.put("message", "user not found");
//				result.put("code", 404);
//				System.out.println("NOT FOUND");
//			}
//		} catch (Exception e) {
//			logger.error("Exception", e);
//			e.printStackTrace();
//		}
//
//		return result;
//	}

	public static List<String> getListOrganization() throws NamingException {
		logger.info("Start getListOrganization");
		if (context==null){
			init();
		}
		ArrayList<String> listOrganization=new ArrayList<String>();
		try{
		NamingEnumeration  results = find("(objectClass=organization)");
		while (results.hasMore()) {
			SearchResult sr2 = (SearchResult) results.next();
			Attributes attrs = sr2.getAttributes();
			listOrganization.add(attrs.get("o").get().toString());
		}
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
		}
		return listOrganization;
	}
	
	public static List<String> getListCountry() throws NamingException {
		logger.info("Start getListCountry");
		if (context==null){
			init();
		}
		ArrayList<String> listCountry=new ArrayList<String>();
		try{
		NamingEnumeration  results = find("(objectClass=country)");
		while (results.hasMore()) {
			SearchResult sr2 = (SearchResult) results.next();
			Attributes attrs = sr2.getAttributes();
			listCountry.add(attrs.get("c").get().toString());
		}
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
		}
		return listCountry;
	}

	public static List<String> getListOrganizationUnitFirstLevel() throws NamingException {
		logger.info("Start getListOrganizationUnitFirstLevel");
		if (context==null){
			init();
		}
		ArrayList<String> listCountry=new ArrayList<String>();
		try{
		NamingEnumeration  results = find("(objectClass=organizationalUnit)");
		while (results.hasMore()) {
			SearchResult sr2 = (SearchResult) results.next();
			Attributes attrs = sr2.getAttributes();
			String fullDistinguishedName = sr2.getName();
			int level = StringUtils.countMatches(fullDistinguishedName, "ou");
			if(level<=1){
				listCountry.add(attrs.get("ou").get().toString());
			}
		}
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
		}
		return listCountry;
	}
	
	public static List<BlocklyAttribute> getAttributes() throws NamingException {
		logger.info("Start getListOrganization");
		if (context==null){
			init();
		}
		ArrayList<BlocklyAttribute> listAttributes=new ArrayList<BlocklyAttribute>();
		listAttributes.add(new BlocklyAttribute("Country", "c", getListCountry()));
		listAttributes.add(new BlocklyAttribute("Organization", "o", getListOrganization()));
		listAttributes.add(new BlocklyAttribute("Unit", "ou", getListOrganizationUnitFirstLevel()));
		listAttributes.add(new BlocklyAttribute("Surname", "sn"));
		listAttributes.add(new BlocklyAttribute("E-mail", "mail"));
		listAttributes.add(new BlocklyAttribute("Common name", "cn"));
		listAttributes.add(new BlocklyAttribute("Role", "vidasrole"));
		return listAttributes;
	}
	
	
	public static void main(String[] args) {
		String rules= PropertiesHelper.getProps().getProperty("Specialist");
		String service="Video";
		String operation="POST";
		String url="";
		String customer="Alitalia";
		try {
			JSONObject jsonRules=new JSONObject(rules);
			if(jsonRules==null || !jsonRules.has(service)){
				System.out.println("SERVICE NOT ALLOW");
				return;
			}
			JSONObject jsonRulesService = jsonRules.optJSONObject(service);
			if(jsonRulesService==null || !jsonRulesService.has(operation)){
				System.out.println("OPERATION NOT ALLOW");
				return;
			}
			String allowOperation= jsonRulesService.optString(operation);
			if(!allowOperation.equals("All")){
				System.out.println("Restrict to Customer");
				url=customer;
			}
			System.out.println(url);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
}
