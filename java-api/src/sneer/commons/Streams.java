package sneer.commons;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Streams {

    public static String readString(InputStream content) throws IOException {
        return new String(readFully(content));
    }

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
