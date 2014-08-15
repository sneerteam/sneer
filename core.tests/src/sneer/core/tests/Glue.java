package sneer.core.tests;

import rx.*;
import sneer.*;
import sneer.tuples.*;
import clojure.java.api.*;
import clojure.lang.*;

class Glue {

	public static TupleSpace newTupleSpace(PublicKey ownPuk, Observable<PublicKey> peers, Object network) {
		return (TupleSpace) sneerCoreVar("reify-tuple-space").invoke(ownPuk, peers, network);
	}

	public static Object newNetwork() {
		try {
			return networkSimulator("new-network").call();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static void tearDownNetwork(Object network) {
		networkSimulator("stop-network").invoke(network);
	}

	private static IFn networkSimulator(String var) {
		return var("sneer.core.tests.network-simulator", var);
	}
	
	public static IFn sneerCoreVar(String simpleName) {
		return var("sneer.core", simpleName);
	}

	private static IFn var(String ns, String simpleName) {
		Clojure.var("clojure.core/require").invoke(Clojure.read(ns));
		return Clojure.var(ns + "/" + simpleName);
	}	
}
