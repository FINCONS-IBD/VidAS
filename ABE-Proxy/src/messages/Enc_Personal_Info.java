package messages;

import java.util.ArrayList;

/*
 * @author Diego Pedone
 * @version 24/05/2017
 */

public class Enc_Personal_Info {
	private String enc_personal_info;
	private ArrayList<Metadata> metadata;
	
	public Enc_Personal_Info(String enc_personal_info, ArrayList<Metadata> metadata){
		this.enc_personal_info = enc_personal_info;
		this.metadata = metadata;
	}

	public String getEnc_personal_info() {
		return enc_personal_info;
	}
	
	public ArrayList<Metadata> getMetadata() {
		return this.metadata;
	}

}
