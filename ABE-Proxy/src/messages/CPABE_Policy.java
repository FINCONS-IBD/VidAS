package messages;

import java.util.ArrayList;

/*
 * @author Salvador Pérez
 * @version 24/11/2016
 */

public class CPABE_Policy {
	private String specs;
	private String url;
	private ArrayList<Metadata> metadata;
	
	public CPABE_Policy(String specs, String url, ArrayList<Metadata> metadata) throws Exception{
		if(specs != null && url != null)
			throw new Exception("The “specs” and “url” are mutually exclusive, i.e. only one of those should be in the JSON.");
		this.specs = specs;
		this.url = url;
		this.metadata = metadata;
	}

	public String getSpecs() {
		return this.specs;
	}

	public String getUrl() {
		return this.url;
	}

	public ArrayList<Metadata> getMetadata() {
		return this.metadata;
	}

}
