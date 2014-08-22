package sneer.core.tests;

import rx.*;
import rx.subjects.*;
import sneer.*;
import sneer.impl.keys.*;
import sneer.tuples.*;

public class TupleSpaceTestsBase extends TestWithNetwork {
	
	protected final PrivateKey userA = Keys.createPrivateKey();
	protected final PrivateKey userB = Keys.createPrivateKey();
	protected final PrivateKey userC = Keys.createPrivateKey();

	protected final TupleSpace tuplesA = newTupleSpace(userA, followees(userB, userC));
	protected final TupleSpace tuplesB = newTupleSpace(userB, followees(userA, userC));
	protected final TupleSpace tuplesC = newTupleSpace(userC, followees(userA, userB));
	
	protected Observable<PublicKey> followees(PrivateKey... followees) {
		return Observable.from(followees).map(PrivateKey.TO_PUBLIC_KEY);
	}

	protected TupleSpace newTupleSpace(PrivateKey ownPrik, Observable<PublicKey> followees) {
		return Glue.newTupleSpace(ownPrik.publicKey(), newTupleBase(), network, followees);
	}

	protected Object newTupleBase() {
		return ReplaySubject.<Tuple>create();
	}	
}