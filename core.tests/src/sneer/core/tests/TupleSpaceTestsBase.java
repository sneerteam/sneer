package sneer.core.tests;

import static sneer.ClojureUtils.var;
import rx.Observable;
import rx.functions.Func0;
import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.crypto.impl.KeysImpl;
import sneer.tuples.TupleSpace;

public class TupleSpaceTestsBase extends TestWithNetwork {
	
	protected final PrivateKey userA = new KeysImpl().createPrivateKey();
	protected final PrivateKey userB = new KeysImpl().createPrivateKey();
	protected final PrivateKey userC = new KeysImpl().createPrivateKey();

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
			return Glue.newPersistentTupleBase();
		}});
	}

	protected Observable<PublicKey> followees(PrivateKey... followees) {
		return Observable.from(followees).map(PrivateKey.TO_PUBLIC_KEY);
	}

	protected TupleSpace newTupleSpace(PrivateKey ownPrik, Observable<PublicKey> followees) {
		PublicKey ownPuk = ownPrik.publicKey();
		final Object base = newTupleBase();
		Glue.networkConnect(network, ownPuk, base);
		return (TupleSpace) var("sneer.tuple.space", "reify-tuple-space").invoke(ownPuk, base);
	}

	protected Object newTupleBase() {
		return tupleBaseFactory.call();
	}	
}