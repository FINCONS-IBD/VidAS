package messages;

import java.util.ArrayList;

/*
 * @author Salvador Pérez
 * @version 11/11/2016
 */

public class Key_Storage {
	private String storage_type;
	private ArrayList<Storage_Parameters> storage_parameters;
	
	public Key_Storage(String storage_type, ArrayList<Storage_Parameters> storage_parameters){
		this.storage_type = storage_type;
		this.storage_parameters = storage_parameters;
	}

	public String getStorage_type() {
		return storage_type;
	}

	public ArrayList<Storage_Parameters> getStorage_parameters() {
		return storage_parameters;
	}

}
