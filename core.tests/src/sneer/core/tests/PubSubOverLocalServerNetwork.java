package sneer.core.tests;

import static sneer.core.tests.ClojureUtils.var;

public class PubSubOverLocalServerNetwork extends PubSubTest {

	@Override
	protected Object newNetwork() {
		return var("sneer.core.tests.local-server-network", "start-local").invoke();
	}
}
