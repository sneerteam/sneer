package sneer.core.tests;

public class PubSubOverServerNetwork extends PubSubTest {
	
	@Override
	protected Object newNetwork() {
		return Glue.var("sneer.core.tests.local-server-network", "start").invoke();
	}

}
