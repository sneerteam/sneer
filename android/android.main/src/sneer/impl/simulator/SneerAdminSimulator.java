package sneer.impl.simulator;

import sneer.*;
import sneer.admin.*;
import sneer.commons.*;

public class SneerAdminSimulator implements SneerAdmin, sneer.crypto.Keys {
	
	private final PrivateKey neidePrik = KeysSimulator.createPrivateKey();

	private final SneerSimulator sneer = new SneerSimulator(neidePrik);

	{
		SystemReport.updateReport("simulator.start");
	}
	
	
	@Override
	public Sneer sneer() {
		return sneer;
	}
	
	
	@Override
	public PrivateKey privateKey() {
		return neidePrik;
	}


	@Override
	public sneer.crypto.Keys keys() {
		return this;
	}


	@Override
	public PublicKey createPublicKey(String bytesAsString) {
		return KeysSimulator.createPublicKey(bytesAsString);
	}

}
