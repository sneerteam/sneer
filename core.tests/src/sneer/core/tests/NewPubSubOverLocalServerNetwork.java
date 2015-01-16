package sneer.core.tests;

import static sneer.ClojureUtils.var;
import rx.Observable;
import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.tuples.TupleSpace;

public class NewPubSubOverLocalServerNetwork extends PubSubTest {

	@Override
	protected Object newNetwork() {
		return var("sneer.core.tests.local-server-network", "start").invoke();
	}

	@Override
	protected TupleSpace newTupleSpace(PrivateKey ownPrik, Observable<PublicKey> followees) {
		PublicKey ownPuk = ownPrik.publicKey();
		final Object base = newTupleBase();
		var("sneer.core.tests.local-server-network", "connect").invoke(network, ownPuk, base);
		return (TupleSpace) var("sneer.tuple.space", "reify-tuple-space").invoke(ownPuk, base);
	}

	@Override
	protected Object newTupleBase() {
		final Object db = var("sneer.tuple.jdbc-database", "create-sqlite-db").invoke();
		return var("sneer.tuple.persistent-tuple-base", "create").invoke(db);
	}

}
