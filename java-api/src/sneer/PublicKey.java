package sneer;

import java.io.Serializable;

public interface PublicKey extends Serializable {
	
	/** This byte[] representation of this public key. */
	byte[] bytes();

	/** This representation of this public key as a hexadecimal string. */
	String bytesAsString();
}
