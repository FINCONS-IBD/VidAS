package messages;

/*
 * @author Diego Pedone
 * @version 31/05/2017
 * 
 */

public class Enc_CPABE_InfoSignBody {
	private String iv;
	private String enc_cpabe;
	
	public Enc_CPABE_InfoSignBody(String iv, String enc_cpabe){
		this.enc_cpabe = enc_cpabe;
		this.iv = iv;
	}

	public String getEnc_cpabe() {
		return enc_cpabe;
	}
	
	public String getIv() {
		return this.iv;
	}

}
