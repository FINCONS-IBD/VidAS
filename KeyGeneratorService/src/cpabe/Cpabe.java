package cpabe;


import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import bswabe.Bswabe;
import bswabe.BswabeMsk;
import bswabe.BswabePrv;
import bswabe.BswabePub;
import bswabe.SerializeUtils;
import cpabe.policy.LangPolicy;

public class Cpabe {

	/**
	 * @param args
	 * @author Junwei Wang(wakemecn@gmail.com)
	 */

	public void setup(String pubfile, String mskfile) throws IOException,
	ClassNotFoundException {
		byte[] pub_byte, msk_byte;
		BswabePub pub = new BswabePub();
		BswabeMsk msk = new BswabeMsk();
		Bswabe.setup(pub, msk);

		/* store BswabePub into mskfile */
		pub_byte = SerializeUtils.serializeBswabePub(pub);
		Common.spitFile(pubfile, pub_byte);

		/* store BswabeMsk into mskfile */
		msk_byte = SerializeUtils.serializeBswabeMsk(msk);
		Common.spitFile(mskfile, msk_byte);
	}

	public String keygen(String pubfile, String mskfile,
			String attr_str) throws NoSuchAlgorithmException, IOException {
		BswabePub pub;
		BswabeMsk msk;
		byte[] pub_byte, msk_byte, prv_byte;
		String json = null;
		try{
			/* get BswabePub from pubfile */
			pub_byte = Common.suckFile(pubfile);
			String base64publicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(pub_byte);


			pub = SerializeUtils.unserializeBswabePub(pub_byte);

			/* get BswabeMsk from mskfile */
			msk_byte = Common.suckFile(mskfile);
			msk = SerializeUtils.unserializeBswabeMsk(pub, msk_byte);

			String[] attr_arr = LangPolicy.parseAttribute(attr_str);
			BswabePrv prv = Bswabe.keygen(pub, msk, attr_arr);

			/* store BswabePrv into prvfile */
			prv_byte = SerializeUtils.serializeBswabePrv(prv);
			String base64privateKey = Base64.getUrlEncoder().withoutPadding().encodeToString(prv_byte);


			json = "{\"code\"=200,\"public\"=\""+base64publicKey+"\", \"private\"=\""+base64privateKey+"\"}";

		}catch(Exception e ){
			e.printStackTrace();
			json = "{\"faultCode\"=500, \"message\"=\"Internal Server Error\"}";
		}

		//Common.spitFile(prvfile, prv_byte);

		return json;
	}

}
