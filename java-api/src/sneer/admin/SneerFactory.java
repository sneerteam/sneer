package sneer.admin;

import java.io.*;

import sneer.commons.exceptions.*;


public interface SneerFactory {

	/** @return The SneerAdmin instance kept in secureFolder or an empty, newly-created SneerAdmin instance with a newly-created private key if secureFolder is empty. */
	SneerAdmin open(File secureFolder) throws FriendlyException;
	
}
