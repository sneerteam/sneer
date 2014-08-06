package sneer;

import sneer.admin.*;
import sneer.commons.exceptions.*;
import sneer.impl.keys.*;
import sneer.refimpl.*;
import sneer.tuples.*;

public class TupleSpaceTestsBase {
	
	private final Object network = createNetwork();

	protected final PrivateKey userA = Keys.createPrivateKey();
	protected final PrivateKey userB = Keys.createPrivateKey();
	protected final PrivateKey userC = Keys.createPrivateKey();

	protected final Sneer sneerA = init(userA);
	protected final TupleSpace tuplesA = sneerA.tupleSpace();

	protected final Sneer sneerB = init(userB);
	protected final TupleSpace tuplesB = sneerB.tupleSpace();

	protected final Sneer sneerC = init(userC);
	protected final TupleSpace tuplesC = sneerC.tupleSpace();
	
	protected void introduce(Sneer a, Sneer b) {
		try {
			a.setContact(nameOf(b), a.produceParty(b.self().publicKey().mostRecent()));
			b.setContact(nameOf(a), b.produceParty(a.self().publicKey().mostRecent()));
		} catch (FriendlyException e) {
			throw new RuntimeException(e);
		}
	}


	private String nameOf(Sneer a) {
		if (sneerA == a) return "a";
		if (sneerB == a) return "b";
		if (sneerC == a) return "c";
		throw new IllegalStateException();
	}

	
	protected SneerAdmin createSneerAdmin(Object session) {
		return new SneerAdminInProcess((LocalTuplesFactory) session);
	}
	
	protected Object createNetwork() {
		return new TuplesFactoryInProcess();
	}
	
	private Sneer init(PrivateKey prik) {
		try {
			SneerAdmin admin = createSneerAdmin(network);
			admin.initialize(prik);
			return admin.sneer();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}