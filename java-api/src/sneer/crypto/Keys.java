package sneer.crypto;

import sneer.PublicKey;

public interface Keys {
	
	/** @see PublicKey.toHex() */
	PublicKey createPublicKey(String hex);
}
