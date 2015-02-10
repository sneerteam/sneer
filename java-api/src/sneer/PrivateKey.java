package sneer;

import rx.functions.Func1;

import java.io.Serializable;

public interface PrivateKey extends Serializable {

	/** The public key corresponding to this private key. */
	PublicKey publicKey();
	
	/** The seed used to create this private key. */
	byte[] toBytes();

	/** This representation of the seed used to create this private key as a hexadecimal string. */
	String toHex();
	
	public static final Func1<PrivateKey, PublicKey> TO_PUBLIC_KEY = new Func1<PrivateKey, PublicKey>() {  @Override public PublicKey call(PrivateKey prik) {
		return prik.publicKey();
	}};
	

}
