package sneer.crypto.impl;

import java.util.Arrays;

import sneer.PublicKey;
import sneer.commons.Codec;

class PublicKeyImpl implements PublicKey {
	

	//THIS MUST BE PRIVATE. A common base class cannot be extracted for PrivateKeyImpl and PublicKeyImpl for security reasons.
	@SuppressWarnings("unused")
	private final java.security.PublicKey delegatePuk;
	private final byte[] bytes;



	public PublicKeyImpl(java.security.PublicKey puk, byte[] bytes) {
		delegatePuk = puk;
		this.bytes = bytes;
	}
	
	

	@Override
	public byte[] toBytes() {
		return bytes;
	}
	
	
	@Override
	public String toHex() {
		return Codec.toHex(bytes);
	}
	

	@Override
	public String toString() {
		return "PUK::" + toHex().substring(0, 5);
	}

	
	@Override
	public int hashCode() {
		return Codec.hashCode(toBytes());
	}

	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof PublicKeyImpl))
			return false;
		PublicKeyImpl other = (PublicKeyImpl) obj;
		return Arrays.equals(bytes, other.bytes);
	}
	
	
	private static final long serialVersionUID = 1L;
}