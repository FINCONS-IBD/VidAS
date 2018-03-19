package com.fincons.h2;

import java.util.Date;

import org.json.JSONObject;

import com.fincons.token.utils.DateUtil;

public class UserForH2 {
	private String username;
	private String userSecret;
	private long challengeID;
	private Date validityTime;
	private Date timestamp;
	
	public UserForH2(){
		
	}
	
	public UserForH2(String username, String userSecret, long challengeID, Date validityTime, Date timestamp) {
		super();
		this.username = username;
		this.userSecret = userSecret;
		this.challengeID = challengeID;
		this.validityTime = validityTime;
		this.timestamp = timestamp;
	}

	public UserForH2(String username, String userSecret, JSONObject userData, Date timestamp) {
		super();
		this.username = username;
		this.userSecret = userSecret;
		this.challengeID = userData.optLong("challengeID");
		String validityTimeString =  userData.optString("validityTime");
		this.validityTime=DateUtil.StringDateToDate(validityTimeString);
		this.timestamp=timestamp;
	}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUserSecret() {
		return userSecret;
	}
	public void setUserSecret(String userSecret) {
		this.userSecret = userSecret;
	}
	public long getChallengeID() {
		return challengeID;
	}
	public void setChallengeID(long challengeID) {
		this.challengeID = challengeID;
	}
	public Date getValidityTime() {
		return validityTime;
	}
	public void setValidityTime(Date validityTime) {
		this.validityTime = validityTime;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "UserForH2 [username=" + username + ", userSecret=" + userSecret + ", challengeID=" + challengeID + ", validityTime=" + validityTime
				+ ", timestamp=" + timestamp + "]";
	}
	
	
	

}

