package sneer.impl.keys;

import java.util.Arrays;

import sneer.*;
import sneer.commons.*;

class PrivateKeyImpl implements PrivateKey { private static final long serialVersionUID = 1L;

	private final byte[] bytes;
	private PublicKeyImpl publicKeyImpl;

	
	PrivateKeyImpl(byte[] bytes) {
		this.bytes = bytes;
		publicKeyImpl = new PublicKeyImpl(bytes);
	}

	
	@Override
	public PublicKey publicKey() {
		return publicKeyImpl;
	}

	
	@Override
	public String toString() {
		return "PRIK:" + Codec.fromUTF8(bytes);
	}

	
	@Override
	public int hashCode() {
		return Arrays.hashCode(bytes);
	}

	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof PrivateKeyImpl))
			return false;
		PrivateKeyImpl other = (PrivateKeyImpl) obj;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		return true;
	}
	
}