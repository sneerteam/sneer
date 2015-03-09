package sneer.core.tests;

import java.util.Arrays;

import rx.functions.Func0;
import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.crypto.impl.KeysImpl;
import sneer.tuples.TupleSpace;

import static sneer.core.tests.ClojureUtils.var;

public class TupleSpaceTestsBase extends TestWithNetwork {

	protected final PrivateKey userA = createPrivateKey("A");
	protected final PrivateKey userB = createPrivateKey("B");
	protected final PrivateKey userC = createPrivateKey("C");

	protected final TupleSpace tuplesA;
	protected final TupleSpace tuplesB;
	protected final TupleSpace tuplesC;

	private final Func0<Object> tupleBaseFactory;

	public TupleSpaceTestsBase(Func0<Object> tupleBaseFactory) {
		this.tupleBaseFactory = tupleBaseFactory;
		tuplesA = newTupleSpace(userA);
		tuplesB = newTupleSpace(userB);
		tuplesC = newTupleSpace(userC);
	}

	public TupleSpaceTestsBase() {
		this(new Func0<Object>() {  @Override public Object call() {
			return Glue.newPersistentTupleBase();
		}});
	}

	protected TupleSpace newTupleSpace(PrivateKey ownPrik) {
		PublicKey ownPuk = ownPrik.publicKey();
		final Object base = newTupleBase();
		Glue.networkConnect(network, ownPuk, base);
		return (TupleSpace) var("sneer.tuple.space", "reify-tuple-space").invoke(ownPuk, base);
	}

	protected Object newTupleBase() {
		return tupleBaseFactory.call();
	}

	private PrivateKey createPrivateKey(String seed) {
		return new KeysImpl().createPrivateKey(Arrays.copyOf(seed.getBytes(), 32));
	}

}