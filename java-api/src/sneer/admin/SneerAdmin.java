package sneer.admin;

import sneer.PrivateKey;
import sneer.Sneer;
import sneer.keys.Keys;

/** This interface will evolve to handle things like multiple devices, public key change and old key repudiation. */
public interface SneerAdmin {

	PrivateKey privateKey();
	
	Sneer sneer();
	
	Keys keys();

}
