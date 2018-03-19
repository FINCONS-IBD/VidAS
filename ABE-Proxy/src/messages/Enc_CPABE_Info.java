package messages;

import java.util.ArrayList;

/*
 * @author Salvador Pérez
 * @version 24/11/2016
 * 
 */

public class Enc_CPABE_Info {
	private String enc_cpabe_info;
	private ArrayList<Metadata> metadata;
	
	public Enc_CPABE_Info(String enc_cpabe_info, ArrayList<Metadata> metadata){
		this.enc_cpabe_info = enc_cpabe_info;
		this.metadata = metadata;
	}

	public String getEnc_cpabe_info() {
		return enc_cpabe_info;
	}
	
	public ArrayList<Metadata> getMetadata() {
		return this.metadata;
	}

}
