package messages;

import java.util.ArrayList;

/*
 * @author Diego Pedone
 * @version 31/05/2017
 * 
 */

public class Enc_CPABE_InfoSign {
	private Enc_CPABE_InfoSignBody enc_cpabe_info;
	private ArrayList<Metadata> metadata;
	private String tag;
	
	public Enc_CPABE_InfoSign(Enc_CPABE_InfoSignBody enc_cpabe_info, ArrayList<Metadata> metadata, String tag){
		this.enc_cpabe_info = enc_cpabe_info;
		this.metadata = metadata;
		this.tag = tag;
	}

	public Enc_CPABE_InfoSignBody getEnc_cpabe_info() {
		return enc_cpabe_info;
	}
	
	public ArrayList<Metadata> getMetadata() {
		return this.metadata;
	}

	public String getTag() {
		return tag;
	}


}
