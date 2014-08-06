package sneer;

import sneer.admin.*;
import sneer.impl.keys.*;

public class InteractionsAPITestsBase {

	private final TuplesFactoryInProcess world = new TuplesFactoryInProcess();
	
	protected final PrivateKey prikA = Keys.createPrivateKey();
	protected final SneerAdmin adminA = newSneerAdmin(prikA);

	private SneerAdminImpl newSneerAdmin(PrivateKey prik) {
		return new SneerAdminImpl(world.newTupleSpace(prik));
	}

}