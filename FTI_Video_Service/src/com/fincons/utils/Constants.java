package com.fincons.utils;

public class Constants {
	
	public static final String LDAP_SERVER = "LdapUrl"; 
	public static final String LDAP_OBJECT_CLASSES = "ldap_obj_classes";
	
	public static final String LDAP_USER = "LdapUser";
	public static final String LDAP_PSW = "LdapPSW";

	public static final String USERNAME = "username";	
	public static final String PROP_GET_USERDATA_URL = "getUserDataServicePath";
	public static final String PATH_FILE_TO_DOWNLOAD = "pathFileToDownload";
	public static final String PATH_FILE = "pathFile";
	public static final String EXTENSION_CRYPTED_FILE = "extensionCryptedFile";
	public static final String ERROR_DECRYPTION = "errorDecryption";
	public static final Object MESSAGE_ERROR_DECRYPRIPTION = "Cannot decrypt, attributes in key do not satisfy policy";
	
	public static final String ATTRIBUTES = "attributes";	
	
	public static final String HMAC_KEY = "HMAC-Key";
	public static final String LDAP_ROOT = "LdapRoot";
	
	public static final String KEY_ALG="client.alg";
	public static final String KEY_ENC="client.enc";
	public static final String KEY_KTY="client.kty";
	public static final String KEY_CRV="client.crv";
	                         
	public static final String ABE_PROXY_IP="client.proxy_ip";
	public static final String ABE_PROXY_PORT="client.proxy_port";
	public static final String ABE_PROXY_ID="client.proxy_id";
	public static final Object ABE_PROXY_PROTOCOL = "client.proxy_protocol";
	                           
	public static final String KEY_STORAGE_ID="client.storage_type";
	public static final String KEY_STORAGE_IP="client.db_ip";
	public static final String KEY_STORAGE_PORT="client.db_port";
	public static final String KEY_STORAGE_DB_DATABASE="client.db_database";
	public static final String KEY_STORAGE_DB_TABLE="client.db_table";
	
}
