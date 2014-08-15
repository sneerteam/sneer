package sneer.core.tests;

import rx.subjects.*;
import sneer.*;
import sneer.admin.*;
import sneer.impl.keys.*;
import sneer.tuples.*;

public class ConversationsAPITestsBase {

//	private final TuplesFactoryInProcess world = new TuplesFactoryInProcess();
	private final Object network = Glue.newNetwork();
	
	protected final PrivateKey userA = newPrivateKey();
	protected final PrivateKey userB = newPrivateKey();
	protected final PrivateKey userC = newPrivateKey();
	
	protected final SneerAdmin adminA = newSneerAdmin(userA);
	protected final SneerAdmin adminB = newSneerAdmin(userB);
	protected final SneerAdmin adminC = newSneerAdmin(userC);
	
	protected final Sneer sneerA = adminA.sneer();
	protected final Sneer sneerB = adminB.sneer();
	protected final Sneer sneerC = adminC.sneer();

	protected PrivateKey newPrivateKey() {
		return Keys.createPrivateKey();
	}

	private SneerAdmin newSneerAdmin(PrivateKey prik) {
		TupleSpace tupleSpace = (TupleSpace) Glue.sneerCoreVar("reify-tuple-space").invoke(prik.publicKey(), PublishSubject.create(), network);
		SneerAdminImpl admin = new SneerAdminImpl(tupleSpace);
		admin.initialize(prik);
		return admin;
	}

}