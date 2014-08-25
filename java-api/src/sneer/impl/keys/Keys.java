package sneer.impl.keys;

import sneer.*;

public class Keys {

	public static PrivateKey createPrivateKey() {
		return new PrivateKeyImpl();
	}

	
	public static PrivateKey createPrivateKey(byte[] bytes) {
		return new PrivateKeyImpl(bytes);
	}
	
	
	public static Object createPrivateKey(String bytesAsString) {
		return new PrivateKeyImpl(bytesAsString);
	}
	
	
	public static PublicKey createPublicKey(byte[] bytes) {
		return new PublicKeyImpl(bytes);
	}

	
	public static PublicKey createPublicKey(String bytesAsString) {
		return new PublicKeyImpl(bytesAsString);
	}

}
