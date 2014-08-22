package sneer.core.tests;

import rx.*;
import sneer.*;
import sneer.impl.keys.*;
import sneer.tuples.*;

public class TupleSpaceTestsBase extends TestWithNetwork {
	
	protected final PrivateKey userA = Keys.createPrivateKey();
	protected final PrivateKey userB = Keys.createPrivateKey();
	protected final PrivateKey userC = Keys.createPrivateKey();

	protected final TupleSpace tuplesA = newTupleSpace(userA, newPeers(userB, userC));
	protected final TupleSpace tuplesB = newTupleSpace(userB, newPeers(userA, userC));
	protected final TupleSpace tuplesC = newTupleSpace(userC, newPeers(userA, userB));
	
	protected Observable<PublicKey> newPeers(PrivateKey... peers) {
		return Observable.from(peers).map(PrivateKey.TO_PUBLIC_KEY);
	}

	protected TupleSpace newTupleSpace(PrivateKey ownPrik, Observable<PublicKey> peers) {
		return Glue.newTupleSpace(ownPrik.publicKey(), peers, network);
	}
	
}