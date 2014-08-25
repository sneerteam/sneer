package sneer;

import java.io.*;

import rx.functions.*;

public interface PrivateKey extends Serializable {

	/** The public key corresponding to this private key. */
	PublicKey publicKey();
	
	/** This byte[] representation of this private key. */
	byte[] bytes();

	/** This representation of this private key as a hexadecimal string. */
	String bytesAsString();
	
	public static final Func1<PrivateKey, PublicKey> TO_PUBLIC_KEY = new Func1<PrivateKey, PublicKey>() {  @Override public PublicKey call(PrivateKey t1) {
		return t1.publicKey();
	}};
	

}
