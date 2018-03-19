package com.fincons.spi;

import java.util.Map;

public interface DBManager{
	/**
	 * Create a connection with the database with the adress and the credentials
	 * @param dataBasePath
	 * 		The address of the Database
	 * @param username
	 * @param password
	 */
	public Integer connect(String dataBasePath, String username, String password);
	
	/**
	 * Execute a query with parameters. A query example could be "SELECT * FROM <a_Table> WHERE id = :id".
	 * Using this format, in the HashMap parameters you have to put a pair <String key, Object value> where the key is what
	 * you find after the colon notation in the query and the Object value is the effective value to assign to the key  
	 * @param query
	 * 		The query in string format as in the previous example ("SELECT * FROM <a_Table> WHERE id = :id")
	 * @param parameters
	 * 		The HashMap<String, Object> where you define the parameters of the query.
	 * @return The result of the query.
	 * @throws Exception 
	 */
	public Object executeQuery(Integer numberConnection, String query, Map<String, Object> parameters) throws Exception;
	
	/**
	 * Disconnects the system from the Database
	 * @param numberConnection Number of DB instance
	 */
	public void disconnect(Integer numberConnection);
}
