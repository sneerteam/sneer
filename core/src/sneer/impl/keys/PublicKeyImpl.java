package sneer.impl.keys;

import java.math.*;

import sneer.*;
import sneer.commons.exceptions.*;

import com.google.bitcoin.core.*;

class PublicKeyImpl implements PublicKey {
	
	//THIS MUST BE PRIVATE. A common base class cannot be extracted for PrivateKeyImpl and PublicKeyImpl.
	private final ECKey ecKey;

	
	PublicKeyImpl(byte[] bytes) {
		this(new ECKey(null, bytes, true));
	}

	
	PublicKeyImpl(String bytesAsString) {
		this(new BigInteger(bytesAsString, 16).toByteArray());
	}


	private PublicKeyImpl(ECKey ecKey) {
		Exceptions.check(!ecKey.hasPrivKey());
		this.ecKey = ecKey;
	}

	
	@Override
	public byte[] bytes() {
		return ecKey.getPubKey();
	}
	
	
	@Override
	public String bytesAsString() {
		return Utils.bytesToHexString(bytes());
	}
	

	@Override
	public String toString() {
		return "PUK::" + bytesAsString().substring(0, 5);
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
		if (!(obj instanceof PublicKeyImpl))
			return false;
		PublicKeyImpl other = (PublicKeyImpl) obj;
		return ecKey.equals(other.ecKey);
	}
	
	
	private static final long serialVersionUID = 1L;


}