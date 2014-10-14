package sneer.core.tests;

import org.junit.Ignore;

import rx.Observable;
import sneer.ClojureUtils;
import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.tuples.TupleSpace;

@Ignore
public class PubSubOverQueuedLocalServerNetwork extends PubSubTest {
	
	@Override	
	protected TupleSpace newTupleSpace(PrivateKey ownPrik, Observable<PublicKey> followees) {
		Object db = ClojureUtils.var("sneer.core.tests.jdbc-tuple-base", "create-sqlite-db").invoke(null);
		Object tupleBase = ClojureUtils.var("sneer.persistent-tuple-base", "create").invoke(db);
		Object fromChannel = null;
		Object toChannel = null;
		Object tupleTransmission = ClojureUtils.var("sneer.tuple-transmission", "start").invoke(tupleBase, db, fromChannel, toChannel, ownPrik.publicKey());
		return Glue.newTupleSpace(ownPrik.publicKey(), tupleBase, network, followees);
	}
	
	
	@Override
	protected Object newNetwork() {
		return ClojureUtils.var("sneer.core.tests.local-server-network", "start-queued").invoke();
	}

}
