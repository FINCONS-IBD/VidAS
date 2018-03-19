package com.fincons.policiesstorage.restlet;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

import com.fincons.policiesstorage.utils.Constants;
import com.fincons.policiesstorage.utils.PropertiesHelper;
import com.fincons.spi.DBManager;
import com.fincons.spi.ImplementationLoader;

public class Dispatcher extends ServerResource {
	final static Logger logger = Logger.getLogger(Dispatcher.class);
	private DBPolicyConnection dbPolicyConnection = DBPolicyConnection.getInstance();

	/**
	 * Gets the entire path of the request
	 * "http://<adress>:<port>/<service>/<items>"
	 * 
	 * @return the items section of the url
	 * @throws UnsupportedEncodingException
	 */
	private String getPath() throws UnsupportedEncodingException {
		String filePath = this.getRequest().getResourceRef().toString();
		String toReplace = this.getRequest().getResourceRef().getBaseRef().toString();
		String offPath = filePath.replaceAll(toReplace, "/");
		return URLDecoder.decode(offPath.replace("//", ""), "utf-8");
	}

	/**
	 * Gets an element from the Database. The element to retrieve is defined in
	 * the path of the request took with getPath method. If the path is empty,
	 * the element to retrieve is the Root node of the Database, else the
	 * elements depends on the path.
	 * 
	 * @return JSON Object with the content of a node if the element refers to a
	 *         directory (either generic directory identified by the ID or an
	 *         Organization identified by the name), else with the information
	 *         about a single node (if the element requested is a policy
	 *         identified by the ID). The JSON Object also contains the status
	 *         code of the request and a message.
	 * @throws UnsupportedEncodingException
	 */
	@Get
	public JsonRepresentation getElement() throws UnsupportedEncodingException {
		logger.info("Start getPolicies from DB");

		JSONObject response = null;
		JSONArray listPolicy = null;
		String path = getPath();
		JSONObject folderContent = null;
		int status = 500;
		String policy = "";
		String blockly_xml = "";
		try {
			response = new JSONObject();
			if (path.equals("/")) {
				logger.info("Folder requested: ROOT");
				JSONObject resultQuery = dbPolicyConnection.getRoot();
				JSONArray data = resultQuery.getJSONArray("data");
				JSONObject rootNode = data.getJSONObject(0);
				String idRoot = rootNode.optString(Constants.ID_NODE);
				folderContent = dbPolicyConnection.getContent(idRoot);
				listPolicy = dbPolicyConnection.createListElement(resultQuery, path);
				response.put("listPolicy", listPolicy);
			} else {
				JSONObject nodeRequested = dbPolicyConnection.getLastNodeFromRelativePath(path);
				if(nodeRequested == null){
					response.put("stauts", 404);
					response.put("message", "One element of the path not found");
					getResponse().setStatus(new Status(404));
					return new JsonRepresentation(response);
				}
				String id = nodeRequested.optString(Constants.ID_NODE);
				String classElement = dbPolicyConnection.getClass(id);
				if (classElement.equalsIgnoreCase(Constants.DIRECTORY_CLASS)) {
					folderContent = dbPolicyConnection.getContent(id);
					listPolicy = dbPolicyConnection.createListElement(folderContent, path);
					response.put("listPolicy", listPolicy);

				} else if (classElement.equalsIgnoreCase(Constants.POLICY_CLASS)) {
					folderContent = dbPolicyConnection.getSingleNode(id);
					logger.debug("Get information about the policy requested");
					JSONArray contentList = folderContent.getJSONArray("data");
					JSONObject policyNode = contentList.getJSONObject(0);
					policy = policyNode.optString(Constants.POLICY_VALUE);
					blockly_xml = policyNode.optString("blockly_xml");
					response.put("policy", policy);
					response.put("xmlPolicy", blockly_xml);
				} else {
					response.put("stauts", 500);
					getResponse().setStatus(new Status(500));
					return new JsonRepresentation(response);
				}
			}
			status = 200;
			response.put("code", status);
		} catch (Exception e) {
			logger.error("Exception", e);
			e.printStackTrace();
			response = new JSONObject();
			try {
				response.put("faultCode", Status.SERVER_ERROR_INTERNAL);
				response.put("message", "Internal Server Error");
			} catch (JSONException e1) {
				logger.error("JSONException", e1);
				e1.printStackTrace();
			}
			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(response);
		}
		getResponse().setStatus(new Status(status));
		logger.info("Get element from DB sucessfull completed." + response);
		return new JsonRepresentation(response);
	}

	/**
	 * Stores an element in the Database. The information about the element are
	 * in the string parameter of the method and in the path took with getPath()
	 * method. The element could be a folder or a policy to create or a policy
	 * to update.
	 * 
	 * @param parameters
	 *            The string with the parameters of the element to create in
	 *            JSON format
	 * @return JSON Object with the status code of the request and a message.
	 * @throws ResourceException
	 * @throws UnsupportedEncodingException
	 * @throws JSONException
	 */
	@Post
	public JsonRepresentation saveElement(String parameters) throws ResourceException, UnsupportedEncodingException {

		logger.info("Start savePolicy (json) : " + parameters);
		String path = getPath();
		logger.debug("Path from getPath(): " + path);
		String[] splittedPath = path.split("/");
		String namePolicy = URLDecoder.decode(splittedPath[splittedPath.length - 1], "utf-8");
		logger.debug("Name policy: " + namePolicy);
		String father = "";
		String policy = "";
		String username = "";
		String type = "";
		String blockly_xml = "";
		String policyID = "";
		try {
			JSONObject obj = new JSONObject(parameters);
			policy = obj.optString(Constants.POLICY);
			username = obj.optString("username");
			if (policy == null || policy == "") {
				type = obj.optString(Constants.TYPE);
			} else {
				blockly_xml = obj.optString("xml_text");
				policyID = obj.optString("id");
			}
		} catch (JSONException e1) {
			logger.error("Error Reading parameter", e1);
			getResponse().setStatus(new Status(400));
			return new JsonRepresentation(new JSONObject());
		}

		JSONObject response = null;
		int status = 500;
		String message = "";
		try {
			response = new JSONObject();
			String[] splitted = path.split("/");
			splitted = Arrays.copyOfRange(splitted, 0, splitted.length - 1);
			path = String.join("/", splitted);
			JSONObject fatherNode = dbPolicyConnection.getLastNodeFromRelativePath(path);
			if(fatherNode == null){
				response.put("stauts", 404);
				response.put("message", "One element of the path not found");
				getResponse().setStatus(new Status(404));
				return new JsonRepresentation(response);
			}
			father = fatherNode.optString(Constants.ID_NODE);
			JSONObject created = null;
			if (!type.isEmpty() || !type.equals("")) {
				created = dbPolicyConnection.saveFolder(father, namePolicy, username);
			} else if (policyID.isEmpty() || policyID.equals("")) {
				if (dbPolicyConnection.isValid(father, namePolicy)) {
					created = dbPolicyConnection.savePolicy(father, namePolicy, policy, username, blockly_xml,
							Constants.DEFAULT_VERSION);
				} else {
					try {
						response = new JSONObject();
						response.put("message", "Conflict, Policy with name " + namePolicy + " already exists");
						response.put("code", 409);
						getResponse().setStatus(new Status(409));
						return new JsonRepresentation(response);
					} catch (JSONException e) {
						logger.error("JSONException", e);
						e.printStackTrace();
					}
				}
			} else {
				created = dbPolicyConnection.updatePolicy(father, namePolicy, policy, username, blockly_xml, policyID);
			}
			
			if (created.optString("status").equalsIgnoreCase("ok")) {
				status = 201;
				response.put("code", status);
			}
			message = created.optString("message");
		} catch (Exception e) {
//			dbPolicyConnection.dbManager.disconnect();
			logger.error("Exception", e);
			e.printStackTrace();
			response = new JSONObject();
			try {
				response.put("faultCode", Status.SERVER_ERROR_INTERNAL);
				response.put("message", "Internal Server Error");
			} catch (JSONException e1) {
				logger.error("JSONException", e1);
				e1.printStackTrace();
			}

			getResponse().setStatus(new Status(500));
			return new JsonRepresentation(response);
		}
		getResponse().setStatus(new Status(status));
		logger.info(message);
		return new JsonRepresentation(response);
	}

	// @Delete
	// public JsonRepresentation deleteNode() {
	// logger.info("Start delete node operation...");
	// String path = getPath();
	// String[] splittedPath = path.split("/");
	// String idNode = splittedPath[splittedPath.length - 1];
	// idNode = "#" + idNode.replace("_", ":");
	// JSONObject response = null;
	// int status = 500;
	// try {
	// response = new JSONObject();
	// startDB();
	// String classNode = getClass(idNode);
	// JSONObject resultQuery = new JSONObject();
	// if (classNode.equalsIgnoreCase(Constants.DIRECTORY_CLASS)) {
	// resultQuery = deleteDirectory(idNode);
	// }
	// else{
	// resultQuery = deletePolicy(idNode);
	// }
	// status = resultQuery.optInt("status");
	// response.put("code", status);
	// } catch (Exception e) {
	// logger.error("Exception", e);
	// e.printStackTrace();
	// response = new JSONObject();
	// try {
	// response.put("faultCode", Status.SERVER_ERROR_INTERNAL);
	// response.put("message", "Internal Server Error");
	// } catch (JSONException e1) {
	// logger.error("JSONException", e1);
	// e1.printStackTrace();
	// }
	//
	// getResponse().setStatus(new Status(500));
	// return new JsonRepresentation(response);
	// } finally {
	// dbManager.disconnect();
	// }
	//
	// getResponse().setStatus(new Status(status));
	// logger.info("Function successfully completed.");
	// return new JsonRepresentation(response);
	// }
	//
	// private JSONObject deletePolicy(String idNode) {
	// return null;
	// }

}
