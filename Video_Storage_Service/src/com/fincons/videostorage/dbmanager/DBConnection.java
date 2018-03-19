package com.fincons.videostorage.dbmanager;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fincons.spi.DBManager;
import com.fincons.spi.ImplementationLoader;
import com.fincons.spi.iOrientDB;
import com.fincons.videostorage.utils.PropertiesHelper;

public class DBConnection {

	final static Logger logger = Logger.getLogger(DBConnection.class);

	// ORIENTDB QUERIES
	private static final String DIRECTORY_CONTENT_QUERY = "select expand(out()) from Video_Directory where @rid = :id";
	private static final String GET_SON_BY_NAME = "select from V where in().@rid contains (@rid=:father) and ((name = :name and @class ='Video_Directory') or (name=:name and @class ='Video'))";
	private static final String CREATE_EDGE = "CREATE EDGE parent_of FROM (SELECT FROM V WHERE @rid = :father AND @class = :classFather) TO (SELECT FROM V WHERE  @rid = :son AND @class = :classSon)";
	private static final String GET_NODE_BY_ID = "SELECT FROM V WHERE @rid = :id";
	private static final String ROOT_NODE_QUERY = "select from V where in().size() == 0 AND @class = :class";
	private static final String CREATE_DIRECTORY = "CREATE VERTEX Video_Directory SET name = :name, timestamp = :timestamp, created_by = :created_by";
	private static final String GETCLASS_QUERY = "SELECT @class FROM V WHERE @rid = :id";
	private static final String CREATE_VIDEO = "CREATE VERTEX Video CONTENT ";
	private static final String ORGANIZATION_NAME_CONNECTED = "SELECT name FROM (TRAVERSE IN() FROM (Select from Directory where @rid=:id)) WHERE in().size() == 1 AND in().name == :root";
	private static final String DELETE_NODE_BY_ID = "DELETE Vertex FROM V WHERE @rid = :id";
	
	private static String usernameDB="";
	private static String passwordDB="";
	private static String pathDB="";
	
	private DBConnection() {
	}

	private static DBConnection instance;
	private iOrientDB dbManager;

	private DBManager getDbManager() {
		return dbManager;
	}

	private void setDbManager(DBManager dbManager) {
		this.dbManager = (iOrientDB) dbManager;
	}

	/**
	 * Create the connection to the DB using the information from the
	 * configuration file
	 * 
	 * @throws Exception
	 */
	public static DBConnection getInstance() throws Exception {
		if (instance == null) {
			instance = new DBConnection();
			String dbAdress = PropertiesHelper.getProps().getProperty(ConstantsDB.DB_ADRESS);
			String dbName = PropertiesHelper.getProps().getProperty(ConstantsDB.DB_NAME);
			usernameDB = PropertiesHelper.getProps().getProperty(ConstantsDB.DB_USER);
			passwordDB = PropertiesHelper.getProps().getProperty(ConstantsDB.DB_PASS);
			instance.setDbManager(ImplementationLoader.getInstance("OrientDBManager"));
			pathDB = "remote:" + dbAdress + "/" + dbName;
		}
		return instance;
	}

	/**
	 * Gets the root node from Database using an OrientDB query
	 * 
	 * @return JSON Object with the information about the root node, the status
	 *         of the query execution and a message
	 */
	public JSONObject getRoot() {
	
			logger.debug("getRoot method start...");
			String query = ROOT_NODE_QUERY;
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("class", ConstantsDB.DIRECTORY_CLASS);
			int con_number=dbManager.connect(pathDB, usernameDB, passwordDB);
			JSONObject result = new JSONObject();
			try {
				result = (JSONObject) dbManager.executeQuery(con_number,query, params);
			} catch (Exception e) {
				dbManager.rollback(con_number);
				logger.error("Error while reading the Root Node", e);
			}
			System.out.println(result.toString());
			dbManager.disconnect(con_number);
			if (result.optString("status") != null && result.optString("status").equals("OK") && result.optJSONArray("data") != null
					&& result.optJSONArray("data").length() > 0) {
				return (JSONObject) result.optJSONArray("data").get(0);
			} else {
				JSONObject resultCreate = createRoot();
				if (resultCreate.optString("status").equals("OK") && resultCreate.has(ConstantsDB.ID_NODE)) {
					return getNodeById(resultCreate.getString(ConstantsDB.ID_NODE));
				} else {
					return null;
				}
			}
	}

	public JSONObject getNodeById(String rid) {
		logger.debug("getNodeById method start...");
		String query = GET_NODE_BY_ID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", rid);
		Integer con_number=dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject result = new JSONObject();
		try {
			result = (JSONObject) dbManager.executeQuery(con_number, query, params);
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while reading Node "+ rid +" from DB", e);
			result.put("status", "ERROR");
			result.put("message", e.getMessage());
		}
		dbManager.disconnect(con_number);
		if (result.optString("status").equals("OK") && result.optJSONArray("data") != null && result.optJSONArray("data").length() > 0) {
			return (JSONObject) result.optJSONArray("data").get(0);
		}
		return result;
	}

	public JSONObject createRoot() {
		logger.debug("createRoot method start...");
		String query = CREATE_DIRECTORY;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("name", PropertiesHelper.getProps().getProperty(ConstantsDB.ROOT_NAME));
		params.put("timestamp", new Date());
		params.put("created_by", "VideoService");
		Integer con_number=dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject result = new JSONObject();
		try {
			result = (JSONObject) dbManager.executeQuery(con_number, query, params);
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while creating the Root Node", e);
			result.put("status", "ERROR");
			result.put("message", e.getMessage());
		}
		dbManager.disconnect(con_number);
		logger.debug(result.toString());
		return result;
	}

	/**
	 * Retrieves the organization name connected to the node selected from
	 * OrientDB using queries
	 * 
	 * @param idNode
	 *            The ID of the node you want to retierve the content
	 * @return JSON Object with the content of the node, the status of the query
	 *         execution and a message
	 */
	public String getOrganizationName(String idNode) {
		logger.debug("getContent method start...");
		JSONObject jsonroot = getRoot();
		String query = ORGANIZATION_NAME_CONNECTED;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", idNode);
		params.put("root", jsonroot.getString(ConstantsDB.ID_NODE));
		Integer con_number=dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject result = new JSONObject();
		try {
			result = (JSONObject) dbManager.executeQuery(con_number, query, params);
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while reading the Organization from Node " + idNode, e);
			result.put("status", "ERROR");
			result.put("message", e.getMessage());
		}
		dbManager.disconnect(con_number);
		if (result.optString("status").equals("OK") && result.optJSONArray("data") != null && result.optJSONArray("data").length() > 0) {
//			JSONObject result = (JSONObject) dbManager.executeQuery(query, params);
			String nameOrg = result.optJSONArray("data").optJSONObject(0).optString("name");
			return nameOrg;
		} else {
			return null;
		}
	}

	public JSONObject createFolderTree(String username, String fullDirPath) {
		String[] dirs = fullDirPath.split("/");
		System.out.println(dirs);
		JSONObject jsonroot = getRoot();
		JSONObject lastNode = null;
		System.out.println(jsonroot.toString());
		String queryCreation = CREATE_DIRECTORY;
		String queryCheck = GET_SON_BY_NAME;
		String queryCreateEdge = CREATE_EDGE;
		HashMap<String, Object> params = new HashMap<String, Object>();

		params.put("father", jsonroot.getString(ConstantsDB.ID_NODE));
		params.put("classFather", jsonroot.getString(ConstantsDB.CLASS_NODE));
		params.put("created_by", username);

		for (String dir : dirs) {
			params.put("name", dir);
			params.put("timestamp", new Date());
			Integer con_number=dbManager.connect(pathDB, usernameDB, passwordDB);
			JSONObject resultCheck = new JSONObject();
			try {
				resultCheck = (JSONObject) dbManager.executeQuery(con_number,queryCheck, params);
			} catch (Exception e) {
				dbManager.rollback(con_number);
				logger.error("Error while checking that Node " + dir + " has sons", e);
				resultCheck.put("status", "ERROR");
				resultCheck.put("message", e.getMessage());
			}
			dbManager.disconnect(con_number);
			// if(resultCheck.has("@rid")){
			if (resultCheck.optString("status").equals("OK") && resultCheck.optJSONArray("data") != null
					&& resultCheck.optJSONArray("data").length() > 0) {
				JSONObject myNode = (JSONObject) resultCheck.optJSONArray("data").get(0);
				params.put("father", myNode.getString(ConstantsDB.ID_NODE));
				params.put("classFather", myNode.getString(ConstantsDB.CLASS_NODE));
				lastNode = myNode;
			} else {
				Integer con_number2=dbManager.connect(pathDB, usernameDB, passwordDB);
				JSONObject result = new JSONObject();
				try {
					result = (JSONObject) dbManager.executeQuery(con_number2, queryCreation, params);
				} catch (Exception e) {
					dbManager.rollback(con_number2);
					logger.error("Error while creating new Directory Node with name " + dir, e);
					result.put("status", "ERROR");
					result.put("message", e.getMessage());
				}
				dbManager.disconnect(con_number2);
				lastNode = result;
				if (result.optString("status").equals("OK")) {
					String currentId = result.getString(ConstantsDB.ID_NODE);
					String currentClass = getClass(currentId);
					params.put("son", currentId);
					params.put("classSon", currentClass);
					Integer con_number3=dbManager.connect(pathDB, usernameDB, passwordDB);
					JSONObject resultEdge = new JSONObject();
					try {
						resultEdge = (JSONObject) dbManager.executeQuery(con_number3, queryCreateEdge, params);
					} catch (Exception e) {
						dbManager.rollback(con_number3);
						logger.error("Error while creating new Edge", e);
						resultEdge.put("status", "ERROR");
						resultEdge.put("message", e.getMessage());
					}
					dbManager.disconnect(con_number3);
					if (!resultEdge.optString("status").equals("OK")) {
						return null;
					}
					params.put("father", currentId);
					params.put("classFather", currentClass);
				} else {
					return null;
				}
			}
		}
		return lastNode;
	}

	public JSONObject getLastNodeFromRelativePath(String relativePath) {
		logger.debug("relativePath" + relativePath);
		// String dirReal=
		// PropertiesHelper.getProps().getProperty(Constants.PATH_FILE);
		// String relativePath=path.replace(dirReal, "");
		if(relativePath.startsWith("/")){
			relativePath=relativePath.replaceFirst("/", "");
		}
		String[] dirs = relativePath.split("/");
	
		// GetRoot
		JSONObject jsonroot = getRoot();
		
		JSONObject lastNode = jsonroot;
		
		String getSon = GET_SON_BY_NAME;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("father", jsonroot.getString(ConstantsDB.ID_NODE));
		if(!relativePath.equals("")){
			for (String dir : dirs) {
				params.put("name", dir);
				logger.debug("dir: "+dir);
				logger.debug("params: "+params);
				logger.debug("getSon: "+getSon);
				Integer con_number=dbManager.connect(pathDB, usernameDB, passwordDB);
				JSONObject resultCheck = new JSONObject();
				try {
					resultCheck = (JSONObject) dbManager.executeQuery(con_number, getSon, params);
				} catch (Exception e) {
					dbManager.rollback(con_number);
					logger.error("Error while reading Node with name " + dir, e);
					resultCheck.put("status", "ERROR");
					resultCheck.put("message", e.getMessage());
				}
				dbManager.disconnect(con_number);
				logger.debug(resultCheck);
				if (resultCheck.optString("status").equals("OK") && resultCheck.optJSONArray("data") != null
						&& resultCheck.optJSONArray("data").length() > 0) {
					JSONObject myNode = (JSONObject) resultCheck.optJSONArray("data").get(0);
					params.put("father", myNode.getString(ConstantsDB.ID_NODE));
					lastNode = myNode;
				} else {
					return null;
				}
			}
		}
		return lastNode;
	}
	
	public boolean createFile(Map<String, Object> param, String relativePathFolder) {

		JSONObject jsonParam = new JSONObject(param);
		String queryCreateVideo = CREATE_VIDEO + jsonParam.toString();
		String queryCreateEdge = CREATE_EDGE;
		HashMap<String, Object> params = new HashMap<String, Object>();
		Integer con_number=dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject restulCreateVideo = new JSONObject();
		try {
			restulCreateVideo = (JSONObject) dbManager.executeQuery(con_number, queryCreateVideo, null);
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while creating Video Node", e);
			restulCreateVideo.put("status", "ERROR");
			restulCreateVideo.put("message", e.getMessage());
		}
		dbManager.disconnect(con_number);

		System.out.println(restulCreateVideo);
		if (restulCreateVideo.optString("status").equals("OK")) {
			String currentId = restulCreateVideo.getString(ConstantsDB.ID_NODE);
			String currentClass = getClass(currentId);
			params.put("son", currentId);
			params.put("classSon", currentClass);
			JSONObject folder = getLastNodeFromRelativePath(relativePathFolder);
			if (folder == null) {
				return false;
			}
			params.put("father", folder.optString(ConstantsDB.ID_NODE));
			params.put("classFather", folder.optString(ConstantsDB.CLASS_NODE));
			Integer con_number2=dbManager.connect(pathDB, usernameDB, passwordDB);			
			JSONObject resultEdge = new JSONObject();
			try {
				resultEdge = (JSONObject) dbManager.executeQuery(con_number2, queryCreateEdge, params);
			} catch (Exception e) {
				dbManager.rollback(con_number2);
				logger.error("Error while creating Edge", e);
				resultEdge.put("status", "ERROR");
				resultEdge.put("message", e.getMessage());
			}
			dbManager.disconnect(con_number2);
			if (resultEdge.optString("status").equals("OK")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Retrieve the class of a single element using OrientDB query
	 * 
	 * @param idNode
	 *            The ID of the element you want to know the class
	 * @return the class of the element
	 */
	private String getClass(String idNode) {
		logger.debug("Check if node selected is a Directory or a Policy");
		String query = GETCLASS_QUERY;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", idNode);
		String classElem = "";
		JSONObject result = new JSONObject();
		Integer con_number=dbManager.connect(pathDB, usernameDB, passwordDB);
		try {
			result = (JSONObject) dbManager.executeQuery(con_number,query, params);
			JSONArray data = result.getJSONArray("data");
			JSONObject elem = data.getJSONObject(0);
			classElem = elem.optString("class");
		} catch (JSONException e) {
			logger.error("JSON Exception " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while reading info about Node " + idNode, e);
			result.put("status", "ERROR");
			result.put("message", e.getMessage());
		}
		dbManager.disconnect(con_number);
		logger.debug("Node selected is a " + classElem);
		return classElem;
	}

	public JSONArray getSubNode(String idPadre) {
		logger.debug("Get SubNode");
		String query = DIRECTORY_CONTENT_QUERY;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", idPadre);
		Integer con_number=dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject result = new JSONObject();
		try {
			result = (JSONObject) dbManager.executeQuery(con_number, query, params);
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while reading info about son Node of Father " + idPadre, e);
			result.put("status", "ERROR");
			result.put("message", e.getMessage());
		}
		dbManager.disconnect(con_number);
		return result.optJSONArray("data");
	}

	/**
	 * 
	 * @param rid
	 * @return
	 */
	public JSONObject deleteNodeById(String rid) {
		logger.debug("getNodeById method start...");
		//Controlla che sia nodo foglia
		JSONArray sons=getSubNode(rid);
		if(!(sons==null || sons.length()==0)){
		 	return new JSONObject().append("status", "ERROR").append("message","It isn't possible remove a node that isn't a leaf node");
		}
//		String query = DELETE_NODE_BY_ID;
//		HashMap<String, Object> params = new HashMap<String, Object>();
//		params.put("id", rid);
		JSONObject nodeToDelete = getNodeById(rid);
		String pathLocalFile = nodeToDelete.optString("real_path_file");
		String idNode = nodeToDelete.optString(ConstantsDB.ID_NODE);
		Integer con_number=dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject result = new JSONObject();
		File localFile = null;
		try {
			result = (JSONObject) dbManager.deleteElement(idNode, con_number);
			localFile = new File(pathLocalFile);
			localFile.delete();
			if(!localFile.exists()){
				dbManager.commit(con_number);
			} else{
				throw new Exception();
			}
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while removing Node  " + rid, e);
			result.put("status", "ERROR");
			result.put("message", e.getMessage());
		}
		dbManager.disconnect(con_number);
//		if (result.optString("status").equals("OK") && result.optJSONArray("data") != null && result.optJSONArray("data").length() > 0) {
//			return (JSONObject) result.optJSONArray("data").get(0);
//		}
		return result;
	}

}
