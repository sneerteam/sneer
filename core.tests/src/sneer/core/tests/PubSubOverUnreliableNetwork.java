package sneer.core.tests;

import org.junit.Ignore;

import sneer.ClojureUtils;

@Ignore("wip")
public class PubSubOverUnreliableNetwork extends PubSubTest {
	
	@Override
	protected Object newNetwork() {
		return ClojureUtils.var("sneer.core.tests.local-server-network", "start").invoke("unreliable");
	}

}
