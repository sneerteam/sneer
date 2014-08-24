package sneer.core.tests;

import org.junit.*;

@Ignore
public class PubSubOverServerNetwork extends PubSubTest {
	
	@Override
	protected Object newNetwork() {
		return Glue.var("sneer.core.tests.local-server-network", "start").invoke();
	}

}
