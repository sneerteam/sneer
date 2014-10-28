package sneer.crypto.impl;

import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.crypto.Keys;

public class KeysImpl implements Keys {

	public PrivateKey createPrivateKey() {
		return new PrivateKeyImpl();
	}

	
	public PrivateKey createPrivateKey(byte[] seed) {
		return new PrivateKeyImpl(seed);
	}
	
	
	public PrivateKey createPrivateKey(String hexSeed) {
		return new PrivateKeyImpl(hexSeed);
	}
	
	
	public PublicKey createPublicKey(byte[] bytes) {
		return new PublicKeyImpl(bytes);
	}

	
	@Override	
	public PublicKey createPublicKey(String hex) {
		return new PublicKeyImpl(hex);
	}

}
