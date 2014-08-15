package sneer.core.tests;

import sneer.*;
import sneer.admin.*;
import sneer.impl.keys.*;

public class ConversationsAPITestsBase {

	private final TuplesFactoryInProcess world = new TuplesFactoryInProcess();
	
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

	private SneerAdminImpl newSneerAdmin(PrivateKey prik) {
		SneerAdminImpl admin = new SneerAdminImpl(world.newTupleSpace(prik));
		admin.initialize(prik);
		return admin;
	}

}