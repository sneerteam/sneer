package sneer.admin;

import sneer.*;

public interface SneerAdmin {

	void initialize(PrivateKey prik);
	
	/** @return The private key used to initialize this Sneer node. */
	PrivateKey privateKey();
	
	/** This must have a privateKey(). */
	void setOwnName(String newName);
	
	/** This must have a privateKey(). */
	Sneer sneer();
	
}
