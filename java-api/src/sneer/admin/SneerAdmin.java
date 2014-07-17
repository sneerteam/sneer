package sneer.admin;

import java.io.*;

import sneer.*;

public interface SneerAdmin {

	boolean isInitialized();
	PrivateKey newPrivateKey();
	void initialize(PrivateKey prik);
	
	/** Must be initialized. */
	Sneer open(PrivateKey prik) throws WrongPrivateKey, IOException;
	
}
