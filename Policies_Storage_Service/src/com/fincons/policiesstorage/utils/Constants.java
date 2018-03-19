package com.fincons.policiesstorage.utils;

public class Constants {
	
	//CONFIG PROPERTIES
	public static final String PATH_FILE = "pathFile";
	public static final String NAME_POLICY = "namePolicy";	
	public static final String LIST_POLICY = "listPolicy";	
	public static final String HMAC_KEY="hmac_key";
	
	//JSON REQUEST PARAMETERS
	public static final String POLICY = "policy";
	public static final String TYPE = "type";
	
	//ORIENTDB QUERIES
	public static final String DIRECTORY_CONTENT_QUERY = "select expand(out()) from Directory where @rid = :id";
	public static final String ROOT_NODE_QUERY = "select from V where in().size() == 0 AND @class = :class";
	public static final String ALL_ORGANIZATION_NODES_QUERY = "select from Directory where in().name = :root";
	public static final String ORGANIZATION_CONTENT_QUERY_BYNAME = "SELECT expand(out()) FROM Directory where name= :name and in().@rid in (select @rid from Directory where in().size() == 0)";
	public static final String GETCLASS_QUERY = "SELECT @class FROM V WHERE @rid = :id";
	public static final String SINGLENODE_QUERY = "SELECT FROM V WHERE @rid = :id";
	public static final String CREATE_POLICY = "CREATE VERTEX Policy SET name = :name, value = :value, timestamp = :timestamp, created_by = :created_by, blockly_xml = :blockly_xml, version = :version";
	public static final String CREATE_DIRECTORY = "CREATE VERTEX Directory SET name = :name, timestamp = :timestamp, created_by = :created_by";
	public static final String DELETE_DIRECTORY = "DELETE VERTEX FROM";
	public static final String CREATE_EDGE = "CREATE EDGE parent_of FROM (SELECT FROM V WHERE @rid = :father AND @class = :classFather) TO (SELECT FROM V WHERE  @rid = :son AND @class = :classSon)";
	public static final String DELETE_EDGE = "DELETE EDGE parent_of FROM (SELECT FROM V WHERE @rid = :father AND @class = :classFather) TO (SELECT FROM V WHERE  @rid = :son AND @class = :classSon)";
	public static final String GET_SON_BY_NAME = "select from V where in().@rid contains (@rid=:father) and name = :name";
	
	//ORIENTDB FIELDS
	public static final String ID_NODE = "@rid";
	public static final String CLASS_NODE = "@class";
	public static final String NAME_NODE = "name";
	public static final String POLICY_VALUE = "value";
	public static final String VERSION_NODE = "version";
	public static final int DEFAULT_VERSION = 1;
	
	public static final String POLICY_CLASS = "Policy";
	public static final String DIRECTORY_CLASS = "Directory";
	
	
	//ORIENTDB CONFIG PROPERTIES
	public static final String DB_NAME = "DB_name";
	public static final String DB_USER = "DB_user";
	public static final String DB_PASS = "DB_password";
	public static final String ROOT_NAME = "DB_rootName";
	public static final String DB_ADRESS = "DB_adress";
	
	
	

}
