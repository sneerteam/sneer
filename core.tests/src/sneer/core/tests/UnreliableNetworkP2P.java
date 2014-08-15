package sneer.core.tests;

public class UnreliableNetworkP2P extends SimpleP2P {
	
	@Override
	protected Object newNetwork() {
		return Glue.var("sneer.core.tests.local-server", "start").invoke("unreliable");
	}
}
