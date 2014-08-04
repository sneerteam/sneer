package sneer.impl.simulator;

import static sneer.commons.exceptions.Exceptions.*;
import sneer.*;
import sneer.admin.*;
import sneer.impl.keys.*;

public class SneerAdminSimulator implements SneerAdmin {

	private PrivateKey privateKey;
	private SneerSimulator sneer;

	
	@Override
	public void initialize(PrivateKey prik) {
		check(privateKey == null);
		privateKey = prik;
		sneer = new SneerSimulator(privateKey);
	}

	
	@Override
	public PrivateKey privateKey() {
		check(privateKey != null);
		return privateKey;
	}


	@Override
	public Sneer sneer() {
		check(sneer != null);
		return sneer;
	}


	public void populate() {
		initialize(Keys.createPrivateKey());
	}


}
