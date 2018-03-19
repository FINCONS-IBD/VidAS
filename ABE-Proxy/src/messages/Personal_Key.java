package messages;

import java.util.ArrayList;

/*
 * @author Diego Pedone
 * @version 25/05/2017
 */

public class Personal_Key {
	private String key;
	private String url;
	private ArrayList<Metadata> metadata;
	
	public Personal_Key(String key, String url, ArrayList<Metadata> metadata) throws Exception{
		if(key != null && url != null)
			throw new Exception("The “specs” and “url” are mutually exclusive, i.e. only one of those should be in the JSON.");
		this.key = key;
		this.url = url;
		this.metadata = metadata;
	}

	public String getKey() {
		return this.key;
	}

	public String getUrl() {
		return this.url;
	}

	public ArrayList<Metadata> getMetadata() {
		return this.metadata;
	}

}
