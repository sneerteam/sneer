package sneer.core.tests;

import org.junit.*;

import rx.*;
import sneer.*;
import sneer.impl.keys.*;
import sneer.tuples.*;

public class TupleSpaceTestsBase {
	
	private final Object network = newNetwork();

	protected final PrivateKey userA = Keys.createPrivateKey();
	protected final PrivateKey userB = Keys.createPrivateKey();
	protected final PrivateKey userC = Keys.createPrivateKey();

	protected final TupleSpace tuplesA = newTupleSpace(userA.publicKey(), newPeers(userB, userC));
	protected final TupleSpace tuplesB = newTupleSpace(userB.publicKey(), newPeers(userA, userC));
	protected final TupleSpace tuplesC = newTupleSpace(userC.publicKey(), newPeers(userA, userB));
	
	private Observable<PublicKey> newPeers(PrivateKey... peers) {
		return Observable.from(peers).map(PrivateKey.TO_PUBLIC_KEY);
	}

	protected Object newNetwork() {
		return Glue.newNetwork();
	}
	
	@After
	public void tearDownNetwork() {
		Glue.tearDownNetwork(network);
	}
	
	private TupleSpace newTupleSpace(PublicKey ownPuk, Observable<PublicKey> peers) {
		return Glue.newTupleSpace(ownPuk, peers, network);
	}
	
}