package sneer.core.tests;

import org.junit.Ignore;

import static sneer.ClojureUtils.var;

@Ignore("avoid polluting the server")
public class PubSubOverServerNetwork extends PubSubTest {

	@Override
	protected Object newNetwork() {
		return var("sneer.core.tests.local-server-network", "start").invoke();
	}

}
