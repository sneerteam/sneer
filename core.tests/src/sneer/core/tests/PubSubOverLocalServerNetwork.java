package sneer.core.tests;

import sneer.ClojureUtils;

public class PubSubOverLocalServerNetwork extends PubSubTest {
	
	@Override
	protected Object newNetwork() {
		return ClojureUtils.var("sneer.core.tests.local-server-network", "start").invoke();
	}

}
