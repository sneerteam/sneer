package core;

import sneer.admin.*;

public class SimpleP2PGlue extends sneer.SimpleP2P {
	@Override
	protected SneerAdmin createSneerAdmin(Object session) {
		return Glue.newSneerAdmin(session);
	}
	
	@Override
	protected Object createSession() {
		return Glue.newSession();
	}
}