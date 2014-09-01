package sneer.core.tests;

import rx.*;
import rx.functions.*;
import rx.subjects.*;
import sneer.*;
import sneer.impl.keys.*;
import sneer.tuples.*;

public class TupleSpaceTestsBase extends TestWithNetwork {
	
	protected final PrivateKey userA = KeysImpl.createPrivateKey();
	protected final PrivateKey userB = KeysImpl.createPrivateKey();
	protected final PrivateKey userC = KeysImpl.createPrivateKey();

	protected final TupleSpace tuplesA;
	protected final TupleSpace tuplesB;
	protected final TupleSpace tuplesC;
	
	private final Func0<Object> tupleBaseFactory;

	public TupleSpaceTestsBase(Func0<Object> tupleBaseFactory) {
		this.tupleBaseFactory = tupleBaseFactory;
		tuplesA = newTupleSpace(userA, followees(userB, userC));
		tuplesB = newTupleSpace(userB, followees(userA, userC));
		tuplesC = newTupleSpace(userC, followees(userA, userB));
	}
	
	public TupleSpaceTestsBase() {
		this(new Func0<Object>() {  @Override public Object call() {
			return ReplaySubject.<Tuple>create();
		}});
	}

	protected Observable<PublicKey> followees(PrivateKey... followees) {
		return Observable.from(followees).map(PrivateKey.TO_PUBLIC_KEY);
	}

	protected TupleSpace newTupleSpace(PrivateKey ownPrik, Observable<PublicKey> followees) {
		return Glue.newTupleSpace(ownPrik.publicKey(), tupleBaseFactory.call(), network, followees);
	}	
}