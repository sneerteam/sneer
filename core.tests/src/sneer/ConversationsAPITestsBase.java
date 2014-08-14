package sneer;

import sneer.admin.*;
import sneer.impl.keys.*;

public class ConversationsAPITestsBase {

	private final TuplesFactoryInProcess world = new TuplesFactoryInProcess();
	
	protected final PrivateKey prikA = newPrivateKey();
	protected final PrivateKey prikB = newPrivateKey();
	protected final PrivateKey prikC = newPrivateKey();
	
	protected final SneerAdmin adminA = newSneerAdmin(prikA);
	protected final SneerAdmin adminB = newSneerAdmin(prikB);
	protected final SneerAdmin adminC = newSneerAdmin(prikC);
	
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