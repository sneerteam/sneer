package org.apache.commons.codec.binary;

import javax.xml.bind.DatatypeConverter;

/**
 * Apache Base64 emulation for transit byte[] representation fully compatible
 * with vanilla transit-java.
 */
public class Base64 {
	
	public static byte[] encodeBase64(byte[] bytes) {
		return DatatypeConverter.printBase64Binary(bytes).getBytes(); //Optimize
	}
	
	public static byte[] decodeBase64(byte[] bytes) {
		return DatatypeConverter.parseBase64Binary(new String(bytes)); //Optimize
	}

}
