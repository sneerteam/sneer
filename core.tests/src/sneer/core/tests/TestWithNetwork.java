package sneer.core.tests;

import org.junit.After;

public class TestWithNetwork {

	protected final Object network = newNetwork();

	protected Object newNetwork() {
		return Glue.newNetworkSimulator();
	}

	@After
	public void tearDownNetwork() {
		Glue.tearDownNetwork(network);
	}

}