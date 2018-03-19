package messages;

/*
 * @author Salvador Pérez
 * @version 24/11/2016
 */

public class Metadata {
	private String name;
	private String value;
	
	public Metadata(String name, String value){
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	public String getValue() {
		return this.value;
	}

}
