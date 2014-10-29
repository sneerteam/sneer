package sneer.crypto.impl;

import java.util.Arrays;

import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.commons.Codec;

class PrivateKeyImpl implements PrivateKey { private static final long serialVersionUID = 1L;

	private final byte[] seed;

	private final PublicKey puk;
	@SuppressWarnings("unused")
	private final java.security.PrivateKey delegatePrik;
	
	
	PrivateKeyImpl(byte[] seed, java.security.PrivateKey prik, java.security.PublicKey puk, byte[] pukBytes) {
		this.seed = seed;
		delegatePrik = prik;
		this.puk = new PublicKeyImpl(puk, pukBytes);
	}


	@Override
	public PublicKey publicKey() {
		return puk;
	}
	
	
	@Override
	public byte[] toBytes() {
		return seed;
	}
	
	
	@Override
	public String toHex() {
		return Codec.toHex(toBytes());
	}

	
	@Override
	public String toString() {
		return "PRIK:" + publicKey().toHex().substring(0, 5);
	}
	

	@Override
	public int hashCode() {
		return Codec.hashCode(seed);
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
		return Arrays.equals(seed, other.seed);
	}

}