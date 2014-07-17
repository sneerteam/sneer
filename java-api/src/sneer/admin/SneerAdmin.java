package sneer.admin;

import java.io.*;

import sneer.*;

public interface SneerAdmin {

	Sneer initialize(PrivateKey prik) throws WrongPrivateKey, IOException;
	
	/** @return The private key used to initialize this Sneer node. The initialize method must have been called. */
	PrivateKey privateKey();
	
	/** The initialize method must have been called. */
	void setOwnName(String newName);
	
}
