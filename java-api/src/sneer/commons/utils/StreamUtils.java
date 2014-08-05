package sneer.commons.utils;

import java.io.*;

public class StreamUtils {

	public static byte[] readFully(InputStream in) throws IOException {
		byte[] b = new byte[8192];
		int read;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while ((read = in.read(b)) != -1) {
			out.write(b, 0, read);
		}
		return out.toByteArray();
	}
	
}
