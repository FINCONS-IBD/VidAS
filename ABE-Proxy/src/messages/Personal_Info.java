package messages;

import java.util.ArrayList;

/*
 * @author Diego Pedone
 * @version 24/05/2017
 */

public class Personal_Info {
	private String timestamp;
	private String device_ID;
	private Personal_Key personalKey;
	private String idSharedSymmetricKey;
	private Key_Storage encrypted_symmetric_key_storage;
	
	public Personal_Info(String timestamp, String device_ID, String idSharedSymmetricKey, Personal_Key personalKey, Key_Storage encrypted_symmetric_key_storage){
		this.timestamp = timestamp;
		this.device_ID = device_ID;
		this.personalKey=personalKey;
		this.idSharedSymmetricKey = idSharedSymmetricKey;
		this.encrypted_symmetric_key_storage = encrypted_symmetric_key_storage;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public String getDevice_ID() {
		return device_ID;
	}

	public Personal_Key getPersonalKey() {
		return personalKey;
	}

	public String getIdSharedSymmetricKey() {
		return idSharedSymmetricKey;
	}
	
	public Key_Storage getEncrypted_symmetric_key_storage() {
		return encrypted_symmetric_key_storage;
	}

}
