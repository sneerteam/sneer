package sneer.impl.keys;

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

	
	public static PublicKey createPublicKey(String bitcoinAddress) {
		//TODO Use the bitcoinj lib.
		return new PublicKeyImpl(Codec.toUTF8(bitcoinAddress));
	}

	
	public static PrivateKey createPrivateKey(String seed) {
		return new PrivateKeyImpl(Codec.toUTF8(seed)); //TODO Use bitcoin keys.
	}

}
