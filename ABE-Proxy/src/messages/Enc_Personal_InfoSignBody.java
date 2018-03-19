package messages;

/*
 * @author Diego Pedone
 * @version 31/05/2017
 * 
 */

public class Enc_Personal_InfoSignBody {
	private String iv;
	private String enc_personal;
	
	public Enc_Personal_InfoSignBody(String iv, String enc_personal){
		this.enc_personal = enc_personal;
		this.iv = iv;
	}

	public String getEnc_Personal() {
		return enc_personal;
	}
	
	public String getIv() {
		return this.iv;
	}

}
