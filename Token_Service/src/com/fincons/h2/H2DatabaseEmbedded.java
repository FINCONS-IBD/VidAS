 package com.fincons.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Logger;
import org.h2.jdbcx.JdbcDataSource;

import com.fincons.token.utils.Constants;
import com.fincons.token.utils.PropertiesHelper;

public class H2DatabaseEmbedded {
	static Connection conn;
//	final static String NAME_TABLE=PropertiesHelper.getProps().getProperty(Constants.H2TABLE).replace(" ", "_");
	
	final static Logger logger = Logger.getLogger(H2DatabaseEmbedded.class);

	public static void createConnection() {
		logger.trace("Called the createConnection...");
		if (conn == null) {
			
			logger.debug(
					"Create new connection. Connection parameters:"+
					"\nDB URL " + PropertiesHelper.getProps().getProperty(Constants.H2SERVER)+
					"\nDB User " + PropertiesHelper.getProps().getProperty(Constants.H2USER)
					);
			
			JdbcDataSource ds = new JdbcDataSource();
			ds.setURL(PropertiesHelper.getProps().getProperty(Constants.H2SERVER));
			ds.setUser(PropertiesHelper.getProps().getProperty(Constants.H2USER));
			ds.setPassword(PropertiesHelper.getProps().getProperty(Constants.H2PASSWORD));
			try {
				conn = ds.getConnection();
//				runStatement("CREATE TABLE IF NOT EXISTS "+NAME_TABLE+" (" + "username VARCHAR PRIMARY KEY, " + "userSecret VARCHAR, " + "validityTime TIMESTAMP, "
//						+ "challengeID BIGINT, " + "timestamp TIMESTAMP "+ ")");
				PreparedStatement pstmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS Users ( username VARCHAR PRIMARY KEY, userSecret VARCHAR, validityTime TIMESTAMP, challengeID BIGINT, timestamp TIMESTAMP )");
				runStatement(pstmt);
			} catch (Exception e) {
				logger.error("createConnection Error ", e);
			}
		}
	}

	public static void closeConnection() {
		logger.trace("Called the closeConnection method...");
		if (conn != null) {
			try {
				PreparedStatement pstmt = conn.prepareStatement("drop table Users");
				runStatement(pstmt);
//				runStatement("drop table "+NAME_TABLE);
				conn.close();
				conn = null;
			} catch (Exception e) {
				logger.error("closeConnection Error ", e);
			}
		}
	}

	static boolean runStatement(PreparedStatement pstmt) {
		logger.trace("Run the runStatement method...");
		logger.debug("Statement: " + pstmt);
		Statement stmt;
		try {
			logger.debug("runStatement: "+pstmt);
//			stmt = conn.createStatement();
			pstmt.execute();
//			stmt.executeUpdate(sqlstmt, nameTable, parameter );
//			stmt.close();
			pstmt.close();
			return true;
		} catch (SQLException sqle) {
			rollback();
			logger.error("SQL ERROR runStatement (rollback performed): "+pstmt , sqle);
			return false;
		}
	}

	public static ResultSet doQuery(PreparedStatement pstmt) {
		logger.trace("Run the doQuery method...");
		try {
			logger.debug("doQuery: "+pstmt);
//			Statement select = conn.createStatement();
			ResultSet result = pstmt.executeQuery();
			return result;
		} catch (Exception e) {
			logger.error("SQL ERROR doQuery :"+pstmt , e);
			rollback();
			return null;
		}
	}

	private static void rollback() {
		logger.trace("Run the rollback method...");
		try {
			conn.rollback();
		} catch (SQLException e) {
			logger.error("SQL ERROR return rollback :" , e);
		}
		
	}

	
}
