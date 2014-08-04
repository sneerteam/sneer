package sneer.admin;

import sneer.*;

public interface SneerAdmin {

	/** @return The private key used to initialize this Sneer node. Null if this has not yet been initialized. */
	PrivateKey privateKey();
	
	void initialize(PrivateKey prik);
	
	/** This must have a privateKey(). */
	Sneer sneer();
	
}
