package sneer;

import sneer.rx.*;

public interface Self extends Individual {

	Observed<PrivateKey> privateKey();
	
	void setName(String newName);
	
}
