package messages;

import com.google.gson.annotations.SerializedName;

public class OrientDB_Result {
	@SerializedName("@type")
	private String type;
	@SerializedName("@rid")
	private String rid;
	@SerializedName("@version")
	private String version;
	private String value;
	
	public String getType() {
		return type;
	}
	public String getRid() {
		return rid;
	}
	public String getVersion() {
		return version;
	}
	public String getValue() {
		return value;
	}
}
