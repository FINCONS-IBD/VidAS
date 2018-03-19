package messages;

import java.util.ArrayList;

/*
 * @author Salvador Pérez
 * @version 11/11/2016
 */

public class CPABE_Private_Key {
	private String cpabe_key;
	private ArrayList<Metadata> metadata;
	
	public CPABE_Private_Key(String cpabe_key, ArrayList<Metadata> metadata){
		this.cpabe_key = cpabe_key;
		this.metadata = metadata;
	}

	public String getCpabe_key() {
		return cpabe_key;
	}
	
	public ArrayList<Metadata> getMetadata() {
		return this.metadata;
	}
}
