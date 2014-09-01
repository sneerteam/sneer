package sneer.impl.keys;

import sneer.*;

public class KeysImpl {

	public static PrivateKey createPrivateKey() {
		return new PrivateKeyImpl();
	}

	
	public static PrivateKey createPrivateKey(byte[] bytes) {
		return new PrivateKeyImpl(bytes);
	}
	
	
	public static PrivateKey createPrivateKey(String bytesAsString) {
		return new PrivateKeyImpl(bytesAsString);
	}
	
	
	public static PublicKey createPublicKey(byte[] bytes) {
		return new PublicKeyImpl(bytes);
	}

	
	public static PublicKey createPublicKey(String bytesAsString) {
		return new PublicKeyImpl(bytesAsString);
	}

}
