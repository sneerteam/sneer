package sneer.core.tests;

import static sneer.ClojureUtils.adminVar;
import static sneer.ClojureUtils.dispose;
import static sneer.ClojureUtils.sneerCoreVar;
import static sneer.ClojureUtils.var;
import rx.Observable;
import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.admin.SneerAdmin;
import sneer.tuples.TupleSpace;

public class Glue {

	public static TupleSpace newTupleSpace(PublicKey ownPuk, Object tupleBase, Object network, Observable<PublicKey> followees) {
		return (TupleSpace) sneerCoreVar("reify-tuple-space").invoke(ownPuk, tupleBase, connect(network, ownPuk), followees);
	}

	private static Object connect(Object network, PublicKey ownPuk) {
		return sneerCoreVar("connect").invoke(network, ownPuk);
	}

	public static Object newNetworkSimulator() {
		return var("sneer.networking.simulator", "create-network").invoke();
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
	
}
