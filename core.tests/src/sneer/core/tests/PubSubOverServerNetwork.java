package sneer.core.tests;

import org.junit.*;

import sneer.*;

@Ignore
public class PubSubOverServerNetwork extends PubSubTest {
	
	@Override
	protected Object newNetwork() {
		return ClojureUtils.var("sneer.core.tests.local-server-network", "start").invoke();
	}

}
