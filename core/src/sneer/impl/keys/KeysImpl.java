package sneer.impl.keys;

import sneer.*;
import sneer.keys.Keys;

public class KeysImpl implements Keys {

	public PrivateKey createPrivateKey() {
		return new PrivateKeyImpl();
	}

	
	public PrivateKey createPrivateKey(byte[] bytes) {
		return new PrivateKeyImpl(bytes);
	}
	
	
	public PrivateKey createPrivateKey(String bytesAsString) {
		return new PrivateKeyImpl(bytesAsString);
	}
	
	
	public PublicKey createPublicKey(byte[] bytes) {
		return new PublicKeyImpl(bytes);
	}

	
	@Override	
	public PublicKey createPublicKey(String bytesAsString) {
		return new PublicKeyImpl(bytesAsString);
	}

}
