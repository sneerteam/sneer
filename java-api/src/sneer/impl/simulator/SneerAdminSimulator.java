package sneer.impl.simulator;

import static sneer.commons.Exceptions.*;
import sneer.*;
import sneer.admin.*;

public class SneerAdminSimulator implements SneerAdmin {

	private PrivateKey privateKey;
	private SneerSimulator sneer;

	@Override
	public Sneer initialize(PrivateKey prik) {
		check(privateKey == null);
		privateKey = prik;
		sneer = new SneerSimulator(privateKey.publicKey());
		return sneer;
	}

	@Override
	public PrivateKey privateKey() {
		check(privateKey != null);
		return privateKey;
	}

	@Override
	public void setOwnName(String newName) {
		sneer.setOwnName(newName);
	}


}
