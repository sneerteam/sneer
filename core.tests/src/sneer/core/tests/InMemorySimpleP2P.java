package sneer.core.tests;

import rx.*;
import sneer.*;
import sneer.tuples.*;

public class InMemorySimpleP2P extends SimpleP2P {
	
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
		return world();
	}	


}
