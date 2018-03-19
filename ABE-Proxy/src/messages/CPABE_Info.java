package messages;

import java.util.ArrayList;

/*
 * @author Salvador Pérez
 * @version 11/11/2016
 * @modified by Diego Pedone
 * @version 26/06/2017
 */

public class CPABE_Info {
	private String timestamp;
	private String device_ID;
	private CPABE_Policy policy;
	private Key_Storage encrypted_symmetric_key_storage;
	
	public CPABE_Info(String timestamp, String device_ID, CPABE_Policy policy, Key_Storage encrypted_symmetric_key_storage){
		this.timestamp = timestamp;
		this.device_ID = device_ID;
		this.policy = policy;
		this.encrypted_symmetric_key_storage = encrypted_symmetric_key_storage;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public String getDevice_ID() {
		return device_ID;
	}

	public CPABE_Policy getPolicy() {
		return policy;
	}

	public Key_Storage getEncrypted_symmetric_key_storage() {
		return encrypted_symmetric_key_storage;
	}

}
