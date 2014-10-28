package sneer.crypto.impl;

import java.math.BigInteger;

import sneer.PrivateKey;
import sneer.PublicKey;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Utils;

class PrivateKeyImpl implements PrivateKey { private static final long serialVersionUID = 1L;

	private final ECKey ecKey;
	
	
	PrivateKeyImpl() {
		this(new ECKey());
	}
	
	
	PrivateKeyImpl(String bytesAsString) {
		this(new BigInteger(bytesAsString, 16).toByteArray());
	}

	
	PrivateKeyImpl(byte[] bytes) {
		this(new ECKey(new BigInteger(1, bytes), null, true));
	}
	
	
	private PrivateKeyImpl(ECKey ecKey) {
		this.ecKey = ecKey;
	}


	@Override
	public PublicKey publicKey() {
		return new PublicKeyImpl(ecKey.getPubKey());
	}
	
	
	@Override
	public byte[] toBytes() {
		return ecKey.getPrivKeyBytes();
	}
	
	
	@Override
	public String toHex() {
		return Utils.bytesToHexString(toBytes());
	}

	
	@Override
	public String toString() {
		return "PRIK:" + publicKey().toHex().substring(0, 5);
	}

	
	@Override
	public int hashCode() {
		return ecKey.hashCode();
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
		return ecKey.equals(other.ecKey);
	}

}