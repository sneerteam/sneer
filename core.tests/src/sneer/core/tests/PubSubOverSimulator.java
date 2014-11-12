package sneer.core.tests;

import org.junit.Ignore;

import rx.Observable;
import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.TuplesFactoryInProcess;
import sneer.tuples.TupleSpace;

@Ignore
public class PubSubOverSimulator extends PubSubTest {

	TuplesFactoryInProcess world;

	private TuplesFactoryInProcess world() {
		if (world == null) {
			world = new TuplesFactoryInProcess();
		}
		return world;
	}


	@Override
	protected TupleSpace newTupleSpace(PrivateKey ownPrik, Observable<PublicKey> followees) {
		return world().newTupleSpace(ownPrik);
	}


	protected Object newTupleBase() {
		throw new IllegalStateException();
	}

}
