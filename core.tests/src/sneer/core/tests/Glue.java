package sneer.core.tests;

import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.admin.SneerAdmin;

import static sneer.core.tests.ClojureUtils.*;

public class Glue {

	public static Object newNetworkSimulator() {
		return var("sneer.core.tests.simulated-network", "start").invoke();
	}
	
	public static void networkConnect(Object network, PublicKey ownPuk, final Object base) {
		var("sneer.core.tests.network", "connect").invoke(network, ownPuk, base);
	}
	
	public static void tearDownNetwork(Object network) {
		dispose(network);
	}

	public static SneerAdmin newSneerAdmin(PrivateKey prik, Object network, Object tupleBase) {
		return (SneerAdmin) adminVar("new-sneer-admin").invoke(prik, network, tupleBase);
	}

	public static SneerAdmin restart(SneerAdmin admin) {
		return (SneerAdmin) adminVar("restart").invoke(admin);
	}

	public static Object newPersistentTupleBase() {
		final Object db = ClojureUtils.var("sneer.tuple.jdbc-database", "create-sqlite-db").invoke();
		return ClojureUtils.var("sneer.tuple.persistent-tuple-base", "create").invoke(db);
	}
	
}
