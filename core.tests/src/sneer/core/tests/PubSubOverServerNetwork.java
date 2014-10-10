package sneer.core.tests;

import org.junit.Ignore;

import sneer.ClojureUtils;

@Ignore("unignore to test against production server")
public class PubSubOverServerNetwork extends PubSubTest {
	
	@Override
	protected Object newNetwork() {
		return ClojureUtils.var("sneer.networking.client", "create-network").invoke();
	}

}
