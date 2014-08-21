package sneer.impl.keys;

import java.util.*;

import sneer.*;

import com.google.common.io.*;

public class Keys {

	private static int nextSeed = 1;

	public static PrivateKey createPrivateKey() {
		return createPrivateKey("" + nextSeed++);
	}
	
	
	public static PublicKey createPublicKey(byte[] bytes) {
		return new PublicKeyImpl(bytes);
	}

	
	public static PublicKey createPublicKey(String bitcoinAddress) {
		//TODO Use the bitcoinj lib.
		return new PublicKeyImpl(BaseEncoding.base64Url().omitPadding().decode(bitcoinAddress));
	}

	
	public static PrivateKey createPrivateKey(String seed) {
		return new PrivateKeyImpl(hash(seed)); //TODO Use bitcoin keys.
	}

	
	protected static byte[] hash(String seed) {
		byte[] ret = new byte[32]; //256bits
		Random random = new Random(seed.hashCode());
		random.nextBytes(ret);
		return ret;
	}

}
