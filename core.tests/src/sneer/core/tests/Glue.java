package sneer.core.tests;

import rx.*;
import sneer.*;
import sneer.admin.*;
import sneer.tuples.*;
import clojure.java.api.*;
import clojure.lang.*;

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

	public static IFn sneerCoreVar(String simpleName) {
		return var("sneer.core", simpleName);
	}

	public static IFn var(String ns, String simpleName) {
		Clojure.var("clojure.core/require").invoke(Clojure.read(ns));
		return Clojure.var(ns + "/" + simpleName);
	}

	public static SneerAdmin newSneerAdmin(PrivateKey prik, Object network, Object tupleBase) {
		return (SneerAdmin) adminVar("new-sneer-admin").invoke(prik, network, tupleBase);
	}

	public static SneerAdmin restart(SneerAdmin admin) {
		return (SneerAdmin) adminVar("restart").invoke(admin);
	}
	
	private static IFn adminVar(String simpleName) {
		return var("sneer.admin", simpleName);
	}
	
}
