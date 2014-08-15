package sneer.core.tests;

public class ServerBasedP2P extends SimpleP2P {
	
	@Override
	protected Object newNetwork() {
		return Glue.var("sneer.core.tests.local-server-network", "start").invoke();
	}

}
