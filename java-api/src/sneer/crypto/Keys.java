package sneer.crypto;

import sneer.PublicKey;

public interface Keys {
	PublicKey createPublicKey(String bytesAsString);
}
