package core;

import sneer.admin.*;

public class SimpleP2PGlue extends sneer.SimpleP2P {
	@Override
	protected SneerAdmin createSneerAdmin() {
		return Glue.newSneerAdmin();
	}
}