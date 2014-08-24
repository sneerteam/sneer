package sneer.core.tests;

import org.junit.*;

import rx.*;
import sneer.*;
import sneer.tuples.*;

@Ignore
public class PubSubOverSimulator extends PubSubTest {
	
	TuplesFactoryInProcess world;

	private TuplesFactoryInProcess world() {
		if (world == null) {
			world = new TuplesFactoryInProcess();
		}
		return world;
	}
	
	protected TupleSpace newTupleSpace(PrivateKey ownPrik, Observable<PublicKey> followees) {
		return world().newTupleSpace(ownPrik);
	}

	protected Object newTupleBase() {
		throw new IllegalStateException();
	}	


}
