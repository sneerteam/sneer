package sneer.impl.simulator;

import sneer.*;
import sneer.admin.*;
import sneer.commons.*;

public class SneerAdminSimulator implements SneerAdmin {

	private final SneerSimulator sneer = new SneerSimulator();

	{
		SystemReport.updateReport("simulator.start");
	}
	
	
	@Override
	public Sneer sneer() {
		return sneer;
	}

}
