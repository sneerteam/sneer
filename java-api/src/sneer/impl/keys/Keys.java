package sneer.impl.keys;

import java.security.SecureRandom;

import sneer.*;
import sneer.commons.*;

public class Keys {

	private static int nextSeed = 1;

	public static PrivateKey createPrivateKey() {
		return createPrivateKey("" + nextSeed++);
	}
	
	
	public static PublicKey createPublicKey(byte[] bytes) {
		return new PublicKeyImpl(bytes);
	}

	
	public static PublicKey createPublicKey(String bitcoinStylePuk) {
		return new PublicKeyImpl(bitcoinStylePuk.getBytes());
	}

	
	public static PrivateKey createPrivateKey(String seed) {
		return new PrivateKeyImpl(randomBytes(seed)); //TODO Use bitcoin keys.
	}

	
	protected static byte[] randomBytes(String seed) {
		byte[] ret = new byte[32]; //256bits
		SecureRandom random = new SecureRandom();
		random.setSeed(Codec.toUTF8(seed));
		random.nextBytes(ret);
		return ret;
	}

}
