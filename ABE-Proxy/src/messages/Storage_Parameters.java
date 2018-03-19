package messages;

/*
 * @author Salvador Pérez
 * @version 11/11/2016
 */

public class Storage_Parameters {
	private String name;
	private String value;
	
	public Storage_Parameters(String name, String value){
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}
	
}
