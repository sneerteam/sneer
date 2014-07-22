package sneer.impl.keys;

import java.util.*;

import sneer.*;

class PublicKeyImpl implements PublicKey {
	
	private final byte[] bytes;

	
	PublicKeyImpl(byte[] bytes) {
		this.bytes = bytes;
	}

	
	@Override
	public String toString() {
		return "PUK:" + bytes[0] + bytes[1] + bytes[2]; //TODO Use same string representation as bitcoin for public address.
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
		if (!(obj instanceof PublicKeyImpl))
			return false;
		PublicKeyImpl other = (PublicKeyImpl) obj;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		return true;
	}
	
	
	private static final long serialVersionUID = 1L;


	@Override
	public byte[] bytes() {
		return bytes.clone();
	}

}