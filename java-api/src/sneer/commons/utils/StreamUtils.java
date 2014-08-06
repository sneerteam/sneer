package sneer.commons.utils;

import java.io.*;

public class StreamUtils {

	public static byte[] readFully(InputStream in) {
		byte[] b = new byte[8192];
		int read;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			while ((read = in.read(b)) != -1) {
				out.write(b, 0, read);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return out.toByteArray();
	}
	
}
