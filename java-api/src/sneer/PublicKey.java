package sneer;

import java.io.Serializable;

public interface PublicKey extends Serializable {
	
	/** This byte[] representation of this public key. */
	byte[] toBytes();

	/** This representation of this public key as a hexadecimal string. */
	String toHex();
}
