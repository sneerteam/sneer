package sneer.keys;

import sneer.PublicKey;

public interface Keys {
	PublicKey createPublicKey(String bytesAsString);
}
