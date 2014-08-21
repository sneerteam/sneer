package sneer.commons;

import java.nio.charset.*;

public class Codec {
	
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	public static byte[] toUTF8(String string) {
		return string.getBytes(UTF_8);
	}

	public static String fromUTF8(byte[] bytes) {
		return new String(bytes, UTF_8);
	}

}
