package sneer.commons;

import java.nio.charset.Charset;

public class Codec {
	
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	public static byte[] toUTF8(String string) {
		return string.getBytes(UTF_8);
	}

	public static String fromUTF8(byte[] bytes) {
		return new String(bytes, UTF_8);
	}
	
	
	final private static char[] hexDigits = "0123456789ABCDEF".toCharArray();
	
	public static String toHex(byte[] bytes) {
		char[] ret = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			ret[j * 2] = hexDigits[v >>> 4];
			ret[j * 2 + 1] = hexDigits[v & 0x0F];
		}
		return new String(ret);
	}

	public static byte[] fromHex(String hex) {
		int len = hex.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2)
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character
					.digit(hex.charAt(i + 1), 16));
		return data;
	}

	
	public static int hashCode(byte[] randomBytes) {
		  int ret = 0;
		  for (int i=0; i<4; i++) {
		    ret <<= 8;
		    ret |= (int)randomBytes[i] & 0xFF;
		  }
		  return ret;
		}
	
}
