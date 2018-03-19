package com.fincons.spi;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientDBManager implements iOrientDB {
	final static Logger logger = Logger.getLogger(OrientDBManager.class);
	final static String CLASS_PROPERTY = "@class";
	final static String ID_PROPERTY = "@rid";

	OrientGraphFactory factory;
	// OrientGraph graph;

	Map<Integer, OrientGraphFactory> connections = new HashMap<Integer, OrientGraphFactory>();

	@Override
	public Integer connect(String DBPath, String username, String password) {
		logger.debug("#### Start connection to DB " + DBPath + " ####");
		try {
			factory = new OrientGraphFactory(DBPath, username, password).setupPool(1, 10);
			OrientGraph verifyConnection = factory.getTx();
			Random rn = new Random();
			Integer randomKey = (int) (rn.nextInt() + new Date().getTime());
			connections.put(randomKey, factory);
			logger.debug("#### Connection successfull to DB " + DBPath + " ####");
			return randomKey;
		} catch (Exception e) {
			logger.error("#### Connection failed to DB " + DBPath + " ####", e);
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Object executeQuery(Integer numberConnection, String query, Map<String, Object> parameters)
			throws Exception {
		OrientGraph graph = connections.get(numberConnection).getTx();
		JSONObject response = null;
		JSONArray vertices = null;
		try {
			response = new JSONObject();

			OCommandSQL sqlQuery = new OCommandSQL(query);
			Object result = graph.command(sqlQuery).execute(parameters);

			logger.debug("#### Get information from query result ####");
			vertices = new JSONArray();
			if (result instanceof Iterable) {
				for (Object item : (Iterable<Object>) result) {
					if (item instanceof Vertex) {
						JSONObject resultJSON = new JSONObject();

						Set<String> attributes = ((Vertex) item).getPropertyKeys();
						for (String elem : attributes) {
							Object o = ((Vertex) item).getProperty(elem);
							if (o instanceof Vertex) {
								Vertex vElem = (Vertex) o;
								resultJSON.put(ID_PROPERTY, vElem.getId().toString());
								resultJSON.put(CLASS_PROPERTY, vElem.getProperty(CLASS_PROPERTY).toString());
							} else {
								resultJSON.put(elem, o);
							}
						}

						if (((Vertex) item).getProperty(CLASS_PROPERTY) != null) {
							resultJSON.put(ID_PROPERTY, ((Vertex) item).getId().toString());
							resultJSON.put(CLASS_PROPERTY, ((Vertex) item).getProperty(CLASS_PROPERTY).toString());
						}
						vertices.put(resultJSON);

						response.put("data", vertices);
						response.put("status", "OK");
						logger.debug("JSON object response successfully created");
					} else {
						JSONObject resultJSON = new JSONObject();

						Set<String> attributes = ((Edge) item).getPropertyKeys();
						for (String elem : attributes) {
							Object o = ((Edge) item).getProperty(elem);
							if (o instanceof Edge) {
								Edge vElem = (Edge) o;
								resultJSON.put(ID_PROPERTY, vElem.getId().toString());
								resultJSON.put(CLASS_PROPERTY, vElem.getProperty(CLASS_PROPERTY).toString());
							} else {
								resultJSON.put(elem, o.toString());
							}
						}

						if (((Edge) item).getProperty(CLASS_PROPERTY) != null) {
							resultJSON.put(ID_PROPERTY, ((Edge) item).getId().toString());
							resultJSON.put(CLASS_PROPERTY, ((Edge) item).getProperty(CLASS_PROPERTY).toString());
						}
						vertices.put(resultJSON);

						response.put("data", vertices);
						response.put("status", "OK");
						response.put("status", "OK");
						logger.debug("JSON object response successfully created");
					}
				}
				return response;
			} else if (result instanceof Vertex) {
				response.put("message", "Vertex successfully created");
				response.put(ID_PROPERTY, ((Vertex) result).getId().toString());
				response.put("status", "OK");
				logger.debug("JSON object response successfully created");
				return response;
			} else if (result instanceof Edge) {
				response.put("message", "Edge successfully created");
				response.put(ID_PROPERTY, ((Edge) result).getId().toString());
				response.put("status", "OK");
				logger.debug("JSON object response successfully created");
				return response;
			} else {
				response.put("message", "Vertex successfully deleted");
				response.put("status", "OK");
				logger.debug("JSON object response successfully created");
				return response;
			}
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public void disconnect(Integer numberConnection) {
		logger.info("Start disconnection from DB...");
		OrientGraphFactory factory = connections.get(numberConnection);
		if (factory != null && !factory.getTx().isClosed()) {
			logger.debug("#### Close Connection to DB  ####");
			factory.getTx().shutdown();
			factory.close();
			connections.remove(numberConnection);
		} else {
			logger.warn("Database is already diconnected");
		}
	}

	@Override
	public void commit(Integer numberConnection) {
		logger.info("Start commit of last-performed transaction...");
		OrientGraphFactory factory = connections.get(numberConnection);
		if (factory != null && !factory.getTx().isClosed()) {
			try {
				OrientGraph graph = factory.getTx();
				graph.commit();
				logger.info("Transaction commit successful");
			} catch (Exception e) {
				throw e;
			}
		} else {
			logger.warn("Could not commit the Transaction. DB could be disconnected");
		}
	}

	@Override
	public void rollback(Integer numberConnection) {
		logger.info("Start rollback operation....");
		OrientGraphFactory factory = connections.get(numberConnection);
		if (factory != null && !factory.getTx().isClosed()) {
			OrientGraph graph = connections.get(numberConnection).getTx();
			System.out.println(graph.toString());
			graph.rollback();
		} else {
			logger.warn("Impossible to perform Rollback Operation. DB could be disconnected");
		}
	}

	@Override
	public Object deleteElement(String id, Integer numberConnection) {
		logger.info("Start delete operation....");
		JSONObject response = null;
		OrientGraphFactory factory = connections.get(numberConnection);
		if (factory != null && !factory.getTx().isClosed()) {
			try {
				response = new JSONObject();
				OrientGraph graph = connections.get(numberConnection).getTx();
				graph.removeVertex(graph.getVertex(id));
				response.put("status", "OK");
				response.put("message", "Deleted node from DB successful");
			} catch (Exception e) {
				throw e;
			}
		} else {
			logger.warn("Impossible to perform Delete Operation. DB could be disconnected");
			response.put("status", "ERROR");
			response.put("message", "Impossible to perform Delete Operation. DB could be disconnected");
		}
		return response;
	}
}
