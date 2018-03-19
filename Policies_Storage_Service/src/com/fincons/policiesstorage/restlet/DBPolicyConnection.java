package com.fincons.policiesstorage.restlet;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fincons.policiesstorage.utils.Constants;
import com.fincons.policiesstorage.utils.PropertiesHelper;
import com.fincons.spi.DBManager;
import com.fincons.spi.ImplementationLoader;
import com.fincons.spi.iOrientDB;

public class DBPolicyConnection {
	final static Logger logger = Logger.getLogger(DBPolicyConnection.class);
	private iOrientDB dbManager;
	private static DBPolicyConnection instance;

	private static String usernameDB = "";
	private static String passwordDB = "";
	private static String pathDB = "";

	private DBPolicyConnection() {

	}

	public DBManager getDBManager() {
		return dbManager;
	}

	public void setDBManager(DBManager dbManager) {
		this.dbManager = (iOrientDB) dbManager;
	}

	public static DBPolicyConnection getInstance() {
		if (instance == null) {
			instance = new DBPolicyConnection();
			String dbAdress = PropertiesHelper.getProps().getProperty(Constants.DB_ADRESS);
			String dbName = PropertiesHelper.getProps().getProperty(Constants.DB_NAME);
			usernameDB = PropertiesHelper.getProps().getProperty(Constants.DB_USER);
			passwordDB = PropertiesHelper.getProps().getProperty(Constants.DB_PASS);
			try {
				instance.setDBManager(ImplementationLoader.getInstance("OrientDBManager"));
			} catch (Exception e) {
				logger.error("Erro in setting instance", e);
				e.printStackTrace();
			}
			pathDB = "remote:" + dbAdress + "/" + dbName;
		}
		return instance;
	}

	/**
	 * Updates the value of a policy inside the Database using an OrientDB query
	 * 
	 * @param father
	 *            The ID of the father node
	 * @param namePolicy
	 *            The name of the policy to modify
	 * @param policy
	 *            The new value of the policy
	 * @param username
	 *            The user that modified the policy
	 * @param blockly_xml
	 *            The xml value of the new policy
	 * @param policyID
	 *            The ID of the old policy
	 * @return JSON Object with the status of the opreation execution and a
	 *         message
	 */
	public JSONObject updatePolicy(String father, String namePolicy, String policy, String username, String blockly_xml,
			String policyPath) {
		logger.info("Update policy \"" + namePolicy + "\" ...");
		String policyID = "";
		String getNodeQuery = Constants.SINGLENODE_QUERY;
		JSONObject policyNode_resultQuery = null;
		JSONObject finalResult = null;
		int version;
		String newPolicyID = "";
		try {
			JSONObject policyNodeRequested = getLastNodeFromRelativePath(policyPath);
			policyID = policyNodeRequested.optString(Constants.ID_NODE);
			HashMap<String, Object> paramsSingleNode = new HashMap<>();
			paramsSingleNode.put("id", policyID);
			finalResult = new JSONObject();
			Integer con_number = dbManager.connect(pathDB, usernameDB, passwordDB);
			try {
				policyNode_resultQuery = (JSONObject) dbManager.executeQuery(con_number, getNodeQuery,
						paramsSingleNode);
			} catch (Exception e2) {
				dbManager.rollback(con_number);
				logger.error("Error while reading Node " + policyID, e2);
				policyNode_resultQuery.put("status", "ERROR");
				policyNode_resultQuery.put("message", e2.getMessage());
			}
			dbManager.disconnect(con_number);
			if (policyNode_resultQuery.optString("status").equalsIgnoreCase("ok")) {
				JSONArray data = policyNode_resultQuery.getJSONArray("data");
				JSONObject policyNode = null;
				if (data != null) {
					policyNode = data.getJSONObject(0);
					version = policyNode.optInt(Constants.VERSION_NODE);
					version++;
					JSONObject createNewPolicy = savePolicy(father, namePolicy, policy, username, blockly_xml, version);
					if (createNewPolicy.optString("status").equalsIgnoreCase("ok")) {
						logger.info("Policy " + namePolicy
								+ " updated. Start delete connection between the old policy and the directory");
						newPolicyID = createNewPolicy.optString("newNode");
						String queryDeleteEdge = Constants.DELETE_EDGE;
						String fatherID = "#" + father.replace('_', ':');
						HashMap<String, Object> paramsDeleteEdge = new HashMap<String, Object>();
						paramsDeleteEdge.put("father", fatherID);
						paramsDeleteEdge.put("classFather", Constants.DIRECTORY_CLASS);
						paramsDeleteEdge.put("son", policyID);
						paramsDeleteEdge.put("classSon", Constants.POLICY_CLASS);
						logger.info("Start delete connection between the old policy and the directory");
						Integer con_number2 = dbManager.connect(pathDB, usernameDB, passwordDB);
						JSONObject edgeDeleteResult = new JSONObject();
						try {
							edgeDeleteResult = (JSONObject) dbManager.executeQuery(con_number2, queryDeleteEdge,
									paramsDeleteEdge);
						} catch (Exception e1) {
							dbManager.rollback(con_number2);
							logger.error("Error while deleting Edge", e1);
							edgeDeleteResult.put("status", "ERROR");
							edgeDeleteResult.put("message", e1.getMessage());
						}
						dbManager.disconnect(con_number2);
						if (edgeDeleteResult.optString("status").equalsIgnoreCase("ok")) {
							String queryCreateEdge = Constants.CREATE_EDGE;
							father = "#" + father.replace('_', ':');
							HashMap<String, Object> paramsEdge = new HashMap<String, Object>();
							paramsEdge.put("father", newPolicyID);
							paramsEdge.put("classFather", Constants.POLICY_CLASS);
							paramsEdge.put("son", policyID);
							paramsEdge.put("classSon", Constants.POLICY_CLASS);
							logger.info("Start create connection between the new version of policy and the old one");
							Integer con_number3 = dbManager.connect(pathDB, usernameDB, passwordDB);
							JSONObject edgeCreateResult = new JSONObject();
							try {
								edgeCreateResult = (JSONObject) dbManager.executeQuery(con_number3, queryCreateEdge,
										paramsEdge);
							} catch (Exception e) {
								dbManager.rollback(con_number3);
								logger.error("Error while creating Edge", e);
								edgeCreateResult.put("status", "ERROR");
								edgeCreateResult.put("message", e.getMessage());
							}
							dbManager.disconnect(con_number3);
							if (edgeCreateResult.optString("status").equalsIgnoreCase("ok")) {
								finalResult.put("status", "OK");
								finalResult.put("message", "Hierarchy successfully updated");
							} else {
								finalResult.put("status", "ERROR");
								finalResult.put("message", "Hierarchy update failed");
							}
						} else {
							finalResult.put("status", "ERROR");
							finalResult.put("message", "Edge not created");
						}
					}
				}
			} else {
				finalResult.put("status", "ERROR");
				finalResult.put("message", "Element not found");
				return finalResult;
			}

		} catch (JSONException e) {
			logger.error("JSON Exception" + e.getMessage());
			e.printStackTrace();
		}
		return finalResult;
	}

	/**
	 * Gets the root node from Database using an OrientDB query
	 * 
	 * @return JSON Object with the information about the root node, the status
	 *         of the query execution and a message
	 */
	public JSONObject getRoot() {
		logger.debug("getRoot method start...");
		String query = Constants.ROOT_NODE_QUERY;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("class", Constants.DIRECTORY_CLASS);
		Integer con_number = dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject result = new JSONObject();
		try {
			result = (JSONObject) dbManager.executeQuery(con_number, query, params);
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while creating Root Node", e);
			try {
				result.put("status", "ERROR");
				result.put("message", e.getMessage());
			} catch (JSONException e1) {
				logger.error("JSON Exception ", e1);
				e1.printStackTrace();
			}
		}
		dbManager.disconnect(con_number);
		System.out.println(result.toString());
		if (result.optString("status") != null && result.optString("status").equals("OK")
				&& result.optJSONArray("data") != null && result.optJSONArray("data").length() > 0) {
			return result;
		} else {
			JSONObject resultCreate = createRoot();
			if (resultCreate.optString("status").equals("OK") && resultCreate.has(Constants.ID_NODE)) {
				try {
					return getSingleNode(resultCreate.getString(Constants.ID_NODE));
				} catch (JSONException e) {
					e.printStackTrace();
					return null;
				}
			} else {
				return null;
			}
		}

	}

	public JSONObject createRoot() {
		logger.debug("createRoot method start...");
		String query = Constants.CREATE_DIRECTORY;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("name", PropertiesHelper.getProps().getProperty(Constants.ROOT_NAME));
		params.put("timestamp", new Date());
		params.put("created_by", "PolicyService");
		Integer con_number = dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject result = new JSONObject();
		try {
			result = (JSONObject) dbManager.executeQuery(con_number, query, params);
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while creating Root Node", e);
			try {
				result.put("status", "ERROR");
				result.put("message", e.getMessage());
			} catch (JSONException e1) {
				logger.error("JSON Exception ", e1);
				e1.printStackTrace();
			}
		}
		dbManager.disconnect(con_number);
		logger.debug(result.toString());
		return result;
	}

	/**
	 * Retrieves the content of a single node from OrientDB using queries
	 * 
	 * @param idNode
	 *            The ID of the node you want to retierve the content
	 * @return JSON Object with the content of the node, the status of the query
	 *         execution and a message
	 */
	public JSONObject getContent(String idNode) {
		logger.debug("getContent method start...");
		String query = Constants.DIRECTORY_CONTENT_QUERY;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", idNode);
		Integer con_number = dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject result = new JSONObject();
		try {
			result = (JSONObject) dbManager.executeQuery(con_number, query, params);
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while reading Directory Node content " + idNode, e);
			try {
				result.put("status", "ERROR");
				result.put("message", e.getMessage());
			} catch (JSONException e1) {
				logger.error("JSON Exception ", e1);
				e1.printStackTrace();
			}
		}
		dbManager.disconnect(con_number);
		return result;
	}

	/**
	 * Gets the content of a node that represents an Organization executing a
	 * query with only the name of the Organization
	 * 
	 * @param name
	 *            The name of the Organization
	 * @return JSON Object with the content of the Organization node, the status
	 *         of the query execution and a message
	 */
	public JSONObject getLimitedFolder(String name) {

		String query = Constants.ORGANIZATION_CONTENT_QUERY_BYNAME;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("name", name);
		Integer con_number = dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject result = new JSONObject();
		try {
			result = (JSONObject) dbManager.executeQuery(con_number, query, params);
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while reading Organization Node " + name, e);
			try {
				result.put("status", "ERROR");
				result.put("message", e.getMessage());
			} catch (JSONException e1) {
				logger.error("JSON Exception ", e1);
				e1.printStackTrace();
			}
		}
		dbManager.disconnect(con_number);
		return result;
	}

	/**
	 * Verifies if the folder selected is an Organization
	 * 
	 * @param folderRequested
	 *            The name of the element or the ID
	 * @return true if @param folderRequested is an Organization, false
	 *         otherwise
	 */
	public boolean isOrganizationName(String folderRequested) {
		logger.debug("Check if the folder requested is an Organization");
		boolean found = false;
		String query = Constants.ALL_ORGANIZATION_NODES_QUERY;
		HashMap<String, Object> params = new HashMap<String, Object>();
		String root = PropertiesHelper.getProps().getProperty(Constants.ROOT_NAME);
		params.put("root", root);
		Integer con_number = dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject organizationNodes = new JSONObject();
		try {
			organizationNodes = (JSONObject) dbManager.executeQuery(con_number, query, params);
		} catch (Exception e1) {
			dbManager.rollback(con_number);
			logger.error("Error while reading Organization Nodes", e1);
			try {
				organizationNodes.put("status", "ERROR");
				organizationNodes.put("message", e1.getMessage());
			} catch (JSONException e) {
				logger.error("JSON Exception ", e);
				e.printStackTrace();
			}
		}
		dbManager.disconnect(con_number);
		String statusQuery = organizationNodes.optString("status");
		if (statusQuery.equalsIgnoreCase("ok")) {
			try {
				JSONArray data = organizationNodes.optJSONArray("data");
				data = (data == null) ? new JSONArray() : data;
				for (int i = 0; i < data.length(); i++) {
					JSONObject temp = data.getJSONObject(i);
					String nameNode = temp.optString("name");
					if (folderRequested.equals(nameNode)) {
						found = true;
						logger.debug("Folder requested is an Organization: " + folderRequested);
						break;
					}
				}
			} catch (JSONException e) {
				logger.error("JSON Exception " + e.getMessage());
				e.printStackTrace();
			}
		}
		if (!found) {
			logger.info("Folder requested is not an Organization.");
		}
		return found;

	}

	/**
	 * Retrieve the class of a single element using OrientDB query
	 * 
	 * @param idNode
	 *            The ID of the element you want to know the class
	 * @return the class of the element
	 */
	public String getClass(String idNode) {
		logger.debug("Check if node selected is a Directory or a Policy");
		String query = Constants.GETCLASS_QUERY;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", idNode);
		String classElem = "";
		try {
			Integer con_number = dbManager.connect(pathDB, usernameDB, passwordDB);
			JSONObject result = new JSONObject();
			try {
				result = (JSONObject) dbManager.executeQuery(con_number, query, params);
			} catch (Exception e) {
				dbManager.rollback(con_number);
				logger.error("Error while reading Node " + idNode, e);
				result.put("status", "ERROR");
				result.put("message", e.getMessage());
			}
			dbManager.disconnect(con_number);
			String statusQuery = result.optString("status");
			if (statusQuery.equalsIgnoreCase("ok")) {
				JSONArray data = result.getJSONArray("data");
				JSONObject elem = data.getJSONObject(0);
				classElem = elem.optString("class");
				logger.debug("Node selected is a " + classElem);
				return classElem;
			} else {
				return null;
			}
		} catch (JSONException e) {
			logger.error("JSON Exception " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Retrieves the information of a single node from the Database using an
	 * OrientDB query
	 * 
	 * @param idNode
	 *            The ID of the node you want to retrieve information
	 * @return JSON Object with the information of the node, the status of the
	 *         query execution and a message
	 */
	public JSONObject getSingleNode(String idNode) {
		String query = Constants.SINGLENODE_QUERY;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", idNode);
		Integer con_number = dbManager.connect(pathDB, usernameDB, passwordDB);
		JSONObject result = new JSONObject();
		try {
			result = (JSONObject) dbManager.executeQuery(con_number, query, params);
		} catch (Exception e) {
			dbManager.rollback(con_number);
			logger.error("Error while reading Node " + idNode, e);
			try {
				result.put("status", "ERROR");
				result.put("message", e.getMessage());
			} catch (JSONException e1) {
				logger.error("JSON Exception ", e1);
				e1.printStackTrace();
			}
		}
		dbManager.disconnect(con_number);
		return result;
	}

	/**
	 * Creates a list in JSON format with the elements inside the folder
	 * parameter. This method takes the JSON Object with the informations about
	 * the content of an OrientDB node and extract a list of elements in JSON
	 * Array format.
	 * 
	 * @param folderContent
	 *            The JSON Object result of the query.
	 * @return JSON Array with the elements contained in the folder node
	 */
	public JSONArray createListElement(JSONObject folderContent, String path) {
		JSONArray list = null;
		try {
			list = new JSONArray();
			logger.debug("Get content of the folder requested");
			JSONArray contentList = folderContent.getJSONArray("data");
			for (int i = 0; i < contentList.length(); i++) {
				JSONObject dataObj = contentList.getJSONObject(i);
				JSONObject temp = new JSONObject();
				temp.put("namePolicy", dataObj.optString("name"));
				String name = dataObj.optString(Constants.NAME_NODE);
				String fullPath = path + "/" + name;
				if (fullPath.startsWith("//")) {
					fullPath = fullPath.replaceFirst("//", "");
				} else if (fullPath.startsWith("/")) {
					fullPath = fullPath.replaceFirst("/", "");
				}
				// idNode = idNode.replace("#", "").replace(":", "_");
				if (dataObj.optString(Constants.CLASS_NODE).equalsIgnoreCase(Constants.DIRECTORY_CLASS)) {

					temp.put("URLDir", fullPath); // .replaceFirst("/",
													// "")
				} else if (dataObj.optString(Constants.CLASS_NODE).equalsIgnoreCase(Constants.POLICY_CLASS)) {
					temp.put("URLPolicy", fullPath); // .replaceFirst("/",
														// "")
					temp.put("xmlPolicy", dataObj.optString("blockly_xml"));
				}
				list.put(temp);
			}
		} catch (JSONException e) {
			logger.error("JSON Exception " + e.getMessage());
			return list;
		}
		return list;
	}

	/**
	 * Saves a policy node inside the Database using an OrientDB query
	 * 
	 * @param father
	 *            The ID of the father node
	 * @param namePolicy
	 *            The name of the policy to create
	 * @param valuePolicy
	 *            The string value of the policy
	 * @param username
	 *            The user that creates the policy
	 * @param blockly_xml
	 *            The xml value of the policy
	 * @param version
	 *            The default version for the first storage of the policy.
	 * @return JSON Object with the information about the new policy created,
	 *         the status of the query execution and a message
	 */
	public JSONObject savePolicy(String father, String namePolicy, String valuePolicy, String username,
			String blockly_xml, int version) {
		logger.info("Start creation policy " + namePolicy);
		JSONObject result = null;
		try {
			String queryAddPolicy = Constants.CREATE_POLICY;
			Date date = new Date();
			Timestamp timestamp = (new Timestamp(date.getTime()));
			HashMap<String, Object> paramsPolicy = new HashMap<String, Object>();
			paramsPolicy.put("name", namePolicy);
			paramsPolicy.put("timestamp", timestamp);
			paramsPolicy.put("value", valuePolicy);
			paramsPolicy.put("created_by", username);
			paramsPolicy.put("blockly_xml", blockly_xml);
			paramsPolicy.put("version", version);
			Integer con_number = dbManager.connect(pathDB, usernameDB, passwordDB);
			JSONObject creationResult = new JSONObject();
			try {
				creationResult = (JSONObject) dbManager.executeQuery(con_number, queryAddPolicy, paramsPolicy);
			} catch (Exception e) {
				dbManager.rollback(con_number);
				logger.error("Error while creating Policy Node with name " + namePolicy, e);
				creationResult.put("status", "ERROR");
				creationResult.put("message", e.getMessage());
			}
			dbManager.disconnect(con_number);
			String newNodeID = creationResult.optString(Constants.ID_NODE);

			result = new JSONObject();
			String statusQuery = creationResult.optString("status");
			if (statusQuery.equalsIgnoreCase("ok")) {
				logger.info("Policy " + namePolicy + " successfully created");
				String queryCreateEdge = Constants.CREATE_EDGE;
				String fatherID = "#" + father.replace('_', ':');
				HashMap<String, Object> paramsEdge = new HashMap<String, Object>();
				paramsEdge.put("father", fatherID);
				paramsEdge.put("classFather", Constants.DIRECTORY_CLASS);
				paramsEdge.put("son", newNodeID);
				paramsEdge.put("classSon", Constants.POLICY_CLASS);
				logger.info("Start create connection between the policy and the directory");
				Integer con_number2 = dbManager.connect(pathDB, usernameDB, passwordDB);
				JSONObject edgeResult = new JSONObject();
				try {
					edgeResult = (JSONObject) dbManager.executeQuery(con_number2, queryCreateEdge, paramsEdge);
				} catch (Exception e) {
					dbManager.rollback(con_number2);
					logger.error("Error while creating Edge", e);
					creationResult.put("status", "ERROR");
					creationResult.put("message", e.getMessage());
				}
				dbManager.disconnect(con_number2);
				if (edgeResult.optString("status").equalsIgnoreCase("ok")) {
					result.put("status", "OK");
					result.put("message", "Policy successfully created");
					result.put("newNode", newNodeID);
				} else {
					result.put("status", "ERROR");
					result.put("message", "Policy creation failed");
				}
			}

		} catch (JSONException e) {
			logger.error("JSON Exception " + e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Saves a folder node inside the Database using an OrientDB query
	 * 
	 * @param father
	 *            The ID of the father node
	 * @param nameFodler
	 *            The name of the folder to create
	 * @param username
	 *            The user that creates the folder
	 * @return JSON Object with the information of the new folder created, the
	 *         status of the query execution and a message.
	 */
	public JSONObject saveFolder(String father, String nameFodler, String username) {
		logger.info("Start creation folder " + nameFodler);
		JSONObject result = null;
		try {
			result = new JSONObject();
			if (isValid(father, nameFodler)) {
				String queryAddFolder = Constants.CREATE_DIRECTORY;
				Date date = new Date();
				Timestamp timestamp = (new Timestamp(date.getTime()));
				HashMap<String, Object> paramsFolder = new HashMap<String, Object>();
				paramsFolder.put("name", nameFodler);
				paramsFolder.put("timestamp", timestamp);
				paramsFolder.put("created_by", username);
				Integer con_number = dbManager.connect(pathDB, usernameDB, passwordDB);
				JSONObject creationResult = new JSONObject();
				try {
					creationResult = (JSONObject) dbManager.executeQuery(con_number, queryAddFolder, paramsFolder);
				} catch (Exception e1) {
					dbManager.rollback(con_number);
					logger.error("Error while creating Directory Node with name " + nameFodler, e1);
					creationResult.put("status", "ERROR");
					creationResult.put("message", e1.getMessage());
				}
				dbManager.disconnect(con_number);
				String newNodeID = creationResult.optString(Constants.ID_NODE);

				String statusQuery = creationResult.optString("status");
				if (statusQuery.equalsIgnoreCase("ok")) {
					logger.info("Folder " + nameFodler + " successfully created");
					String queryCreateEdge = Constants.CREATE_EDGE;
					String fatherID = "#" + father.replace('_', ':');
					HashMap<String, Object> paramsEdge = new HashMap<String, Object>();
					paramsEdge.put("father", fatherID);
					paramsEdge.put("classFather", Constants.DIRECTORY_CLASS);
					paramsEdge.put("son", newNodeID);
					paramsEdge.put("classSon", Constants.DIRECTORY_CLASS);
					logger.info("Start create connection between the father directory and the new directory");
					Integer con_number2 = dbManager.connect(pathDB, usernameDB, passwordDB);
					JSONObject edgeResult = new JSONObject();
					try {
						edgeResult = (JSONObject) dbManager.executeQuery(con_number2, queryCreateEdge, paramsEdge);
					} catch (Exception e) {
						dbManager.rollback(con_number2);
						logger.error("Error while creating Edge", e);
					}
					dbManager.disconnect(con_number2);
					if (edgeResult.optString("status").equalsIgnoreCase("ok")) {
						result.put("status", "OK");
						result.put("message", "Folder successfully created");
					} else {
						result.put("status", "ERROR");
						result.put("message", "Folder creation failed");
					}
				}
			} else {
				result.put("status", "ERROR");
				result.put("message", "Folder with name " + nameFodler + " already exist");
			}
		} catch (JSONException e) {
			logger.error("JSON Exception " + e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Checks if a policy is inside a folder node.
	 * 
	 * @param father
	 *            The ID of the folder node
	 * @param nameNode
	 *            The name of the policy
	 * @return true = if one of the element inside the father node has the same
	 *         name, false = otherwise
	 */
	public boolean isValid(String father, String nameNode) {
		boolean valid = true;
		try {
			father = "#" + father.replace("_", ":");
			JSONObject content = getContent(father);
			JSONArray elementsInside = content.getJSONArray("data");
			for (int i = 0; i < elementsInside.length(); i++) {
				JSONObject temp = elementsInside.getJSONObject(i);
				String name = temp.optString(Constants.NAME_NODE);
				if (name.equalsIgnoreCase(nameNode)) {
					valid = false;
					break;
				}
			}
		} catch (JSONException e) {
			logger.error("JSON Exception " + e.getMessage());
			e.printStackTrace();
		}
		return valid;
	}

	public JSONObject getLastNodeFromRelativePath(String relativePath) throws JSONException {
		logger.debug("relativePath " + relativePath);
		// String dirReal=
		// PropertiesHelper.getProps().getProperty(Constants.PATH_FILE);
		// String relativePath=path.replace(dirReal, "");
		if (relativePath.startsWith("/")) {
			relativePath = relativePath.replaceFirst("/", "");
		}
		String[] dirs = relativePath.split("/");

		// GetRoot
		JSONObject jsonroot = getRoot().optJSONArray("data").getJSONObject(0);
		JSONObject lastNode = jsonroot;
		try {
			String getSon = Constants.GET_SON_BY_NAME;
			HashMap<String, Object> params = new HashMap<String, Object>();

			params.put("father", jsonroot.getString(Constants.ID_NODE));

			int i = 0;
			if (jsonroot.optString("name").equals(dirs[0])) {
				i = 1;
			}

			for (int j = i; j < dirs.length; j++) {
				String dir = dirs[j];

				params.put("name", dir);
				Integer con_number = dbManager.connect(pathDB, usernameDB, passwordDB);
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
					params.put("father", myNode.getString(Constants.ID_NODE));
					lastNode = myNode;
				} else {
					logger.error("resultCheck invalid");
					logger.error("Son with name " + dir + " not found");
					return null;
				}
			}
		} catch (JSONException e) {
			logger.error("JSON Exception " + e.getMessage());
			e.printStackTrace();
		}
		return lastNode;
	}

	// private JSONObject deleteDirectory(String idNode) {
	// logger.info("Delete directory with ID: " + idNode + " from DB");
	// JSONObject result = null;
	// int status = 500;
	// String message = "";
	// try {
	// String query = Constants.DELETE_DIRECTORY + "(" +
	// Constants.SINGLENODE_QUERY + ")";
	// HashMap<String, Object> params = new HashMap<>();
	// params.put("id", idNode);
	// result = (JSONObject) dbManager.executeQuery(query, params);
	// if (result.optString("status").equalsIgnoreCase("ok")) {
	// logger.info("Directory successfully deleted");
	// status = 200;
	// message = "Directory successfully deleted";
	// } else {
	// logger.error("Directory not deleted");
	// message = "Directory not deleted";
	// }
	// result.put("status", status);
	// result.put("message", message);
	// } catch (JSONException e) {
	// logger.error("JSON Exception " + e);
	// e.printStackTrace();
	// }
	// return result;
	// }
}
