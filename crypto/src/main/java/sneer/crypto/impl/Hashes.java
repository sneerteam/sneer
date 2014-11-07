package sneer.crypto.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hashes {

	public static byte[] sha256(byte[] input) {
		return sha256().digest(input);
	}


	public static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
