package messages;

/*
 * Based on RFC 7517 - JSON Web Key (JWK)
 * @author Salvador Pérez
 * @version 04/11/2016
 */

public class JSON_Web_Key {
	private String kty;
	private String crv;
	private String x;
	private String y;
	
	public JSON_Web_Key(String kty, String crv, String x, String y){
		this.kty = kty;
		this.crv = crv;
		this.x =x;
		this.y = y;
	}
	
	public String getKty() {
		return kty;
	}
	public String getCrv() {
		return crv;
	}
	public String getX() {
		return x;
	}
	public String getY() {
		return y;
	}
	
}
