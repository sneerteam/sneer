package org.apache.commons.codec.binary;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;

/** Replacement for apache.commons.codec.Base64 class used by transit-java. The original apache.commons.codec lib includes native libs and Android doesn't like that. :( */
public class Base64 {
	
	private static final Charset US_ASCII = Charset.forName("US-ASCII");
	

	public static byte[] encodeBase64(byte[] bytes) {
		String string = DatatypeConverter.printBase64Binary(bytes);
		return string.getBytes(US_ASCII); //Optimize: avoid conversion to String.
	}
	
	public static byte[] decodeBase64(byte[] bytes) {
		String string = new String(bytes, US_ASCII);
		return DatatypeConverter.parseBase64Binary(string); //Optimize: avoid conversion to String.
	}

}
