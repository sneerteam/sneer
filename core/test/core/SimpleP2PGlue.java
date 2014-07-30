package core;

import sneer.admin.*;

public class SimpleP2PGlue extends sneer.SimpleP2P {
	
	{
		introduce(sneerA, sneerB);
		introduce(sneerA, sneerC);
		introduce(sneerC, sneerB);
	}
	
	@Override
	protected SneerAdmin createSneerAdmin(Object session) {
		return Glue.newSneerAdmin(session);
	}
	
	@Override
	protected Object createNetwork() {
		return Glue.newNetwork();
	}
}