package sneer.core.tests;

public class SimpleP2POverServerNetwork extends SimpleP2P {
	
	@Override
	protected Object newNetwork() {
		return Glue.var("sneer.core.tests.local-server-network", "start").invoke();
	}

}
