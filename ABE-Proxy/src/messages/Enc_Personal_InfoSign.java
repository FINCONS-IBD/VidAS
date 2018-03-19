package messages;

import java.util.ArrayList;

/*
 * @author Diego Pedone
 * @version 31/05/2017
 * 
 */

public class Enc_Personal_InfoSign {
	private Enc_Personal_InfoSignBody enc_personal_info;
	private ArrayList<Metadata> metadata;
	private String tag;
	
	public Enc_Personal_InfoSign(Enc_Personal_InfoSignBody enc_personal_info, ArrayList<Metadata> metadata, String tag){
		this.enc_personal_info = enc_personal_info;
		this.metadata = metadata;
		this.tag = tag;
	}

	public Enc_Personal_InfoSignBody getEnc_Personal_info() {
		return enc_personal_info;
	}
	
	public ArrayList<Metadata> getMetadata() {
		return this.metadata;
	}

	public String getTag() {
		return tag;
	}


}
