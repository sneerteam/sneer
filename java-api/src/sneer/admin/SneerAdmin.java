package sneer.admin;

import sneer.*;

public interface SneerAdmin {

	void initialize(PrivateKey prik);
	
	/** @return The private key used to initialize this Sneer node. Null if this has not yet been initialized. */
	PrivateKey privateKey();
	
	/** This must have a privateKey(). */
	Sneer sneer();
	
}
