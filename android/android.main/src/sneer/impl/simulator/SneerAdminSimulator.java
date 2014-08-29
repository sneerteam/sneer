package sneer.impl.simulator;

import sneer.*;
import sneer.admin.*;
import sneer.commons.*;
import sneer.impl.keys.*;

public class SneerAdminSimulator implements SneerAdmin {
	
	private final PrivateKey neidePrik = Keys.createPrivateKey();

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

}
