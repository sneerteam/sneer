package sneer.impl.simulator;

import static sneer.commons.Exceptions.*;

import java.io.*;

import sneer.*;
import sneer.admin.*;

public class SneerAdminSimulator implements SneerAdmin {

	private PrivateKey prik;
	private SneerSimulator sneer;

	@Override
	public Sneer initialize(PrivateKey prik) throws WrongPrivateKey, IOException {
		this.prik = prik;
		this.sneer = new SneerSimulator(prik.publicKey());
		return this.sneer;
	}

	@Override
	public PrivateKey privateKey() {
		check(prik != null);
		return prik;
	}

	@Override
	public void setOwnName(String newName) {
		sneer.setOwnName(newName);
	}


}
