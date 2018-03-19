package algorithms;

import cpabe.Cpabe;

/*
 * @author Salvador Pérez
 * @version 11/11/2016
 */

public class CPABE {
	private Cpabe cpabe_instance;
	
	public CPABE(){
		this.cpabe_instance = new Cpabe();
	}

	public byte[][] CPABE_setup() throws Exception{
		return this.cpabe_instance.setup();
	}
	
	public byte[] CPABE_generate_key(byte[] pub_key, byte[] msk_key, String attr) throws Exception{
		return this.cpabe_instance.keygen(pub_key, msk_key, attr);
	}
	
	public byte[][] CPABE_encrypt(byte[] pub_key, String policy, byte[] input) throws Exception{
		return this.cpabe_instance.enc(pub_key, policy, input);
	}
	
	public byte[] CPABE_decrypt(byte[] pub_key, byte[] cpabe_key, byte[][] input) throws Exception{
		return this.cpabe_instance.dec(pub_key, cpabe_key, input);
	}
}