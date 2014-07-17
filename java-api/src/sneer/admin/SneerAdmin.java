package sneer.admin;

import sneer.*;
import sneer.commons.exceptions.*;

public interface SneerAdmin {

	Sneer initialize(PrivateKey prik) throws FriendlyException;
	
	/** @return The private key used to initialize this Sneer node. The initialize method must have been called. */
	PrivateKey privateKey();
	
	/** The initialize method must have been called. */
	void setOwnName(String newName);
	
}
