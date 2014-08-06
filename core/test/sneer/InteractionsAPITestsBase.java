package sneer;

import sneer.admin.*;
import sneer.impl.keys.*;

public class InteractionsAPITestsBase {

	private final TuplesFactoryInProcess world = new TuplesFactoryInProcess();
	
	protected final PrivateKey prikA = newPrivateKey();
	protected final PrivateKey prikB = newPrivateKey();
	
	protected final SneerAdmin adminA = newSneerAdmin(prikA);
	protected final SneerAdmin adminB = newSneerAdmin(prikB);

	protected PrivateKey newPrivateKey() {
		return Keys.createPrivateKey();
	}

	private SneerAdminImpl newSneerAdmin(PrivateKey prik) {
		SneerAdminImpl admin = new SneerAdminImpl(world.newTupleSpace(prik));
		admin.initialize(prik);
		return admin;
	}

}