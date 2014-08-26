package org.apache.commons.codec.binary;

/**
 * Apache Base64 emulation for transit byte[] representation fully compatible
 * with vanilla transit-java.
 */
public class Base64 {
	
	public static byte[] encodeBase64(byte[] bytes) {
		return org.spongycastle.util.encoders.Base64.encode(bytes);
	}
	
	public static byte[] decodeBase64(byte[] bytes) {
		return org.spongycastle.util.encoders.Base64.decode(bytes);
	}

}
