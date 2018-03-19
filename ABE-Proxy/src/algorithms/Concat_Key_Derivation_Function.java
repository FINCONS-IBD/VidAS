package algorithms;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/*
 * Based on NIST Special Publication 800-56A
 * @author Salvador Pérez
 * @version 11/11/2016
 */

/**
 * An implementation of Concatenation Key Derivation Function (aka Concat KDF or ConcatKDF)
 * from Section 5.8.1 of National Institute of Standards and Technology (NIST),
 * "Recommendation for Pair-Wise Key Establishment Schemes Using Discrete Logarithm Cryptography",
 * NIST Special Publication 800-56A, Revision 2, May 2013.
 */
public class Concat_Key_Derivation_Function{
	private MessageDigest md;

    public Concat_Key_Derivation_Function() throws NoSuchAlgorithmException {
    	md = MessageDigest.getInstance("SHA-256");
    }
    
    public byte[] intToFourBytes(int i) {
        byte[] res = new byte[4];
        res[0] = (byte) (i >>> 24);
        res[1] = (byte) ((i >>> 16) & 0xFF);
        res[2] = (byte) ((i >>> 8) & 0xFF);
        res[3] = (byte) (i & 0xFF);
        return res;
    }
	
	public byte[] concatKDF(byte[] z, int keyDataLen, byte[] algorithmID, byte[] partyUInfo, byte[] partyVInfo, byte[] suppPubInfo, byte[] suppPrivInfo) {
		try {
		    if (keyDataLen % 8 != 0)
		        throw new IllegalArgumentException("keydatalen should be a multiple of 8");
	        if (algorithmID == null || partyUInfo == null || partyVInfo == null)
	            throw new NullPointerException("Required parameter is null");

	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(algorithmID);
            baos.write(partyUInfo);
            baos.write(partyVInfo);
            if (suppPubInfo != null)
                baos.write(suppPubInfo);
            if (suppPrivInfo != null)
                baos.write(suppPrivInfo);
	        byte[] otherInfo = baos.toByteArray();
	        
	        return concatKDF(z, keyDataLen, otherInfo);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
    }

    private byte[] concatKDF(byte[] z, int keyDataLen, byte[] otherInfo) {
        keyDataLen = keyDataLen / 8;
        byte[] key = new byte[keyDataLen];

        int hashLen = md.getDigestLength();
        int reps = keyDataLen / hashLen;

        for (int i = 0; i <= reps; i++) {
        	md.reset();
            md.update(intToFourBytes(i + 1));
//            System.out.println("i+1"+Arrays.toString(intToFourBytes(i + 1)) );
            md.update(z);
//            System.out.println("z"+Arrays.toString(z) );
            md.update(otherInfo);
//            System.out.println("otherInfo"+Arrays.toString(otherInfo));

            byte[] hash = md.digest();
//            System.out.println("hash"+Arrays.toString(hash));
            if (i < reps)
                System.arraycopy(hash, 0, key, hashLen * i, hashLen);
            else
                System.arraycopy(hash, 0, key, hashLen * i, keyDataLen % hashLen);
        }

//        System.out.println("key"+Arrays.toString(key));
        return key;
    }
}