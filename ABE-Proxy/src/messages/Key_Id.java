package messages;

import java.util.ArrayList;

/*
 * @author Salvador P�rez
 * @version 25/11/2016
 */

public class Key_Id {
	private String enc_sym_key_id;
	private Key_Storage storage;
	private ArrayList<Metadata> metadata;
	
	public Key_Id(String enc_sym_key_id, Key_Storage storage, ArrayList<Metadata> metadata){
		this.enc_sym_key_id = enc_sym_key_id;
		this.metadata = metadata;
		this.storage = storage;
	}

	public String getEnc_sym_key_id() {
		return this.enc_sym_key_id;
	}
	
	public ArrayList<Metadata> getMetadata() {
		return this.metadata;
	}
	
	public Key_Storage getStorage() {
		return this.storage;
	}
	
}
