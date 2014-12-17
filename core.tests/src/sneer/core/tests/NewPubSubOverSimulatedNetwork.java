package sneer.core.tests;

import static sneer.ClojureUtils.var;
import clojure.lang.IFn;
import rx.Observable;
import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.tuples.TupleSpace;

public class NewPubSubOverSimulatedNetwork extends PubSubTest {

	@Override
	protected Object newNetwork() {
		return networkVar("start").invoke();
	}

	@Override
	protected TupleSpace newTupleSpace(PrivateKey ownPrik, Observable<PublicKey> followees) {
		PublicKey ownPuk = ownPrik.publicKey();
		final Object base = newTupleBase();
		networkVar("connect").invoke(network, ownPuk, base);
		return (TupleSpace) var("sneer.tuple.space", "reify-tuple-space").invoke(ownPuk, base);
	}

	private IFn networkVar(String name) {
		return var("sneer.core.tests.simulated-network", name);
	}

	@Override
	protected Object newTupleBase() {
		final Object db = var("sneer.tuple.jdbc-database", "create-sqlite-db").invoke();
		return var("sneer.tuple.persistent-tuple-base", "create").invoke(db);
	}

}
