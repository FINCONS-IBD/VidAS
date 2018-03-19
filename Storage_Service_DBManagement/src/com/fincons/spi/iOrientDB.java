package com.fincons.spi;

/**
 * OrientDB inteface that implements the general interface with main DB
 * Management operations. It exposes operations 
 * 
 * @author gaetano.giordano
 *
 */
public interface iOrientDB extends DBManager {
	/**
	 * Commit operation on the DB
	 * 
	 * @param numberConnection
	 *            The connection identifier
	 */
	public void commit(Integer numberConnection);

	/**
	 * Rollback operation on the DB
	 * 
	 * @param numberConnection
	 *            The connection identifier
	 */
	public void rollback(Integer numberConnection);

	/**
	 * Delete operation on the DB
	 * 
	 * @param id
	 *            Identifier of the element to delete
	 * @param numberConnection
	 *            The connection identifier
	 * 
	 * @return Response object
	 */
	public Object deleteElement(String id, Integer numberConnection);
}
