package messages;

import java.util.ArrayList;

/*
 * @author Salvador Pérez
 * @version 24/11/2016
 */

public class CPABE_Public_Key {
	private String public_parameters;
	private ArrayList<Metadata> metadata;
	
	public CPABE_Public_Key(String public_parameters, ArrayList<Metadata> metadata){
		this.public_parameters = public_parameters;
		this.metadata = metadata;
	}

	public String getPublic_Parameters() {
		return this.public_parameters;
	}
	
	public ArrayList<Metadata> getMetadata() {
		return this.metadata;
	}
	
}
