package sneer.core.tests;

import sneer.*;
import sneer.admin.*;
import sneer.impl.keys.*;

public class ConversationsAPITestsBase extends TestWithNetwork {

	protected final SneerAdmin adminA = newSneerAdmin();
	protected final SneerAdmin adminB = newSneerAdmin();
	protected final SneerAdmin adminC = newSneerAdmin();

	protected final PublicKey userA = adminA.sneer().self().publicKey().current();
	protected final PublicKey userB = adminB.sneer().self().publicKey().current();
	protected final PublicKey userC = adminC.sneer().self().publicKey().current();
	
	protected final Sneer sneerA = adminA.sneer();
	protected final Sneer sneerB = adminB.sneer();
	protected final Sneer sneerC = adminC.sneer();

	protected PrivateKey newPrivateKey() {
		return Keys.createPrivateKey();
	}
	
	private SneerAdmin newSneerAdmin() {
		return Glue.newSneerAdmin(Keys.createPrivateKey(), network);
	}

}