package sneer.core.tests;

import org.junit.*;

@Ignore("wip")
public class PubSubOverUnreliableNetwork extends PubSubTest {
	
	@Override
	protected Object newNetwork() {
		return Glue.var("sneer.core.tests.local-server-network", "start").invoke("unreliable");
	}
}
