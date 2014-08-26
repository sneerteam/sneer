package sneer.core.tests;

import static sneer.ClojureUtils.*;
import rx.*;
import sneer.*;
import sneer.admin.*;
import sneer.tuples.*;

class Glue {

	public static TupleSpace newTupleSpace(PublicKey ownPuk, Object tupleBase, Object network, Observable<PublicKey> followees) {
		return (TupleSpace) sneerCoreVar("reify-tuple-space").invoke(ownPuk, tupleBase, connect(network, ownPuk), followees);
	}

	public static Object connect(Object network, PublicKey ownPuk) {
		return sneerCoreVar("connect").invoke(network, ownPuk);
	}

	public static Object newNetworkSimulator() {
		return var("sneer.networking.simulator", "new-network").invoke();
	}
	
	public static void tearDownNetwork(Object network) {
		sneerCoreVar("dispose").invoke(network);
	}

	public static SneerAdmin newSneerAdmin(PrivateKey prik, Object network, Object tupleBase) {
		return (SneerAdmin) adminVar("new-sneer-admin").invoke(prik, network, tupleBase);
	}

	public static SneerAdmin restart(SneerAdmin admin) {
		return (SneerAdmin) adminVar("restart").invoke(admin);
	}
	
}
