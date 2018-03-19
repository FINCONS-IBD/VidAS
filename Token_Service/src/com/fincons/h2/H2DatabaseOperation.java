package com.fincons.h2;

import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class H2DatabaseOperation {
	
	final static Logger logger = Logger.getLogger(H2DatabaseOperation.class);
	
	public static boolean checkUser(String username) {
		logger.trace("Called the checkUser method...");

//		ResultSet result = H2DatabaseEmbedded.doQuery("SELECT count(*) as numUtenti FROM "+H2DatabaseEmbedded.NAME_TABLE+" where username='" + username + "'");
		try {
			PreparedStatement pstmt = H2DatabaseEmbedded.conn.prepareStatement( "SELECT count(*) as numUtenti FROM  Users where username=?" );
			pstmt.setString( 1 ,username);
			ResultSet result =  H2DatabaseEmbedded.doQuery(pstmt);
			result.first();
			if(result.getInt("numUtenti")==1){
				return true;
			}
		} catch (SQLException e) {
			logger.error("SQL ERROR getUserForH2FromResult ", e);
			return false;
		}
		return false;
	}

	public static UserForH2 selectUserByUsername(String username) {
		logger.trace("Called the selectUserByUsername method...");
//		ResultSet result = H2DatabaseEmbedded.doQuery("SELECT * FROM "+H2DatabaseEmbedded.NAME_TABLE+" where username='" + username + "'");
		PreparedStatement pstmt;
		try {
			pstmt = H2DatabaseEmbedded.conn.prepareStatement( "SELECT * FROM Users where username=?" );
			pstmt.setString( 1 ,username);
			ResultSet result =  H2DatabaseEmbedded.doQuery(pstmt);
			
			UserForH2 user = getUserForH2FromResult(result);
			return user;
		} catch (SQLException e) {
			logger.error("SQL ERROR selectUserByUsername ", e);
			return null;
		}
		
	}

	public static boolean deleteUserFromUsername(String username) {
		logger.trace("Called the deleteUserFromUsername method...");
		try {
			PreparedStatement pstmt = H2DatabaseEmbedded.conn.prepareStatement( "delete from Users where username=?" );
			pstmt.setString( 1 ,username);
			
			return H2DatabaseEmbedded.runStatement(pstmt);
		} catch (SQLException e) {
			logger.error("SQL ERROR deleteUserFromUsername ", e);
			return false;
		}
	}
	
	public static boolean insertUser(UserForH2 user) {
		logger.trace("Called the insertUser method...");
		if(!checkUser(user.getUsername())){		
			boolean inserimento=false;
			try {
				PreparedStatement pstmt = H2DatabaseEmbedded.conn.prepareStatement( "insert into Users values ( ?, ?, ?, ? ,?)" );
				pstmt.setString( 1 ,user.getUsername());
				pstmt.setString( 2 ,user.getUserSecret());
				pstmt.setTimestamp( 3,  new Timestamp(user.getValidityTime().getTime()));
				pstmt.setLong( 4, user.getChallengeID());
				pstmt.setTimestamp( 5,  new Timestamp(user.getTimestamp().getTime()));
				inserimento=H2DatabaseEmbedded.runStatement(pstmt);
			} catch (SQLException e) {
				logger.error("SQL ERROR deleteUserFromUsername ", e);
				return inserimento;
			}
//			boolean inserimento=H2DatabaseEmbedded.runStatement("insert into "+H2DatabaseEmbedded.NAME_TABLE+" values ( '" + user.getUsername() + "', '" + user.getUserSecret() + "', '"
//					+ new Timestamp(user.getValidityTime().getTime()) + "', " + user.getChallengeID()+ ", '" +new Timestamp(user.getTimestamp().getTime())+ "')");
			if(inserimento){
				logger.info("New User "+user.getUsername()+" Inserted");
				return true;
			}else{
				logger.debug("Impossible to insert user " + user.getUsername() +" in H2 Database");
				return false;
			}
		}else{
			logger.debug("User Exist in H2 Database");
			return false;
		}
	}

	private static UserForH2 getUserForH2FromResult(ResultSet result) {
		logger.trace("Called the getUserForH2FromResult method...");
		UserForH2 userForH2 = null;
		try {
			while (result.next()) { // process results one row at a time
				userForH2 = new UserForH2(result.getString("username"), result.getString("userSecret"), result.getLong("challengeID"),	result.getTimestamp("validityTime"), result.getTimestamp("timestamp"));
			}
		} catch (SQLException e) {
			logger.error("SQL ERROR getUserForH2FromResult ", e);
		}finally{
			return userForH2;
		}
	}
	
	public static List<UserForH2> selectAll() {
		try
		{
		//		ResultSet result = H2DatabaseEmbedded.doQuery("SELECT * FROM "+H2DatabaseEmbedded.NAME_TABLE);
			PreparedStatement pstmt = H2DatabaseEmbedded.conn.prepareStatement( "SELECT * FROM Users" );
			ResultSet result =  H2DatabaseEmbedded.doQuery(pstmt);
			List<UserForH2> users = getAllUserForH2FromResult(result);
			return users;
		} catch (SQLException e) {
			logger.error("SQL ERROR selectUserByUsername ", e);
			return null;
		}
	}
	private static List<UserForH2> getAllUserForH2FromResult(ResultSet result) {
		List<UserForH2> users= new ArrayList<UserForH2>();
		UserForH2 userForH2 = null;
		try {
			while (result.next()) { // process results one row at a time
				userForH2 = new UserForH2(result.getString("username"), result.getString("userSecret"),	result.getLong("challengeID"),	result.getTimestamp("validityTime"), result.getTimestamp("timestamp"));
				users.add(userForH2);
			}
		} catch (SQLException e) {
			logger.error("SQL ERROR getUserForH2FromResult ", e);
		}finally{
			return users;
			
		}
	}

}
