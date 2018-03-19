package messages;

import com.google.gson.annotations.SerializedName;

public class OrientDB_Saved_Key {
	@SerializedName("@class")
	private String _class;
	private String id;
	private String value;
	private String encryptor;
	private String encryption_date;
	private String enc_algorithms;
	private String expirate_date;
	
	public OrientDB_Saved_Key(String _class, String id, String value, String encryption_date, String encryption_algorithms, String encryptor, String expiration_date){
		this._class = _class;
		this.id = id;
		this.value = value;
		this.encryption_date = encryption_date;
		this.enc_algorithms = encryption_algorithms;
		this.encryptor = encryptor;
		this.expirate_date = expiration_date;
	}
	
	public String get_class() {
		return _class;
	}
	public String getId() {
		return id;
	}
	public String getValue() {
		return value;
	}
	public String getEncryptor() {
		return encryptor;
	}
	public String getEncryption_date() {
		return encryption_date;
	}
	public String getEnc_algorithms() {
		return enc_algorithms;
	}
	public String getExpirate_date() {
		return expirate_date;
	}
}
