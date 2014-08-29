package sneer.impl.keys;

import java.math.*;

import sneer.*;

import com.google.bitcoin.core.*;

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
	public byte[] bytes() {
		return ecKey.getPrivKeyBytes();
	}
	
	
	@Override
	public String bytesAsString() {
		return Utils.bytesToHexString(bytes());
	}

	
	@Override
	public String toString() {
		return "PRIK:" + publicKey().bytesAsString().substring(0, 5);
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


	//Spike:
	public static void main(String[] args) {
	    ECKey key = new ECKey();
	    System.out.println("We created key:\n" + key);
	    System.out.println(key.getPubKey().length);
	    System.out.println(key.getPrivKeyBytes().length);
	    System.out.println("\n\nHere with private:\n" + key.toStringWithPrivate());

	    System.out.println("\nKey1 priv: " + Utils.bytesToHexString(key.getPrivKeyBytes()));
	    System.out.println("Key1 pub : " + Utils.bytesToHexString(key.getPubKey()));

	    ECKey key2 = new ECKey(new BigInteger(1, key.getPrivKeyBytes()), null, true);
	    System.out.println("\nKey2 priv: " + Utils.bytesToHexString(key2.getPrivKeyBytes()));
	    System.out.println("Key2 pub : " + Utils.bytesToHexString(key2.getPubKey()));

	    ECKey key3 = new ECKey(key.getPrivKeyBytes(), null);
	    System.out.println("\nKey3 priv: " + Utils.bytesToHexString(key3.getPrivKeyBytes()));
	    System.out.println("Key3 pub : " + Utils.bytesToHexString(key3.getPubKey())); //Uncompressed :(
	}

}