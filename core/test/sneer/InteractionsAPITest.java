package sneer;

import static org.junit.Assert.*;
import static sneer.ObservableTestUtils.*;

import org.junit.*;

import sneer.impl.keys.*;

public class InteractionsAPITest extends InteractionsAPITestsBase {

	@Test
	public void changesPrik() {
		
		assertEquals(prikA, adminA.privateKey());
		
		PrivateKey anotherPrikInTheFireWall = Keys.createPrivateKey();
		adminA.initialize(anotherPrikInTheFireWall);
		
		assertEquals(anotherPrikInTheFireWall, adminA.privateKey());
	}
	
	@Test
	public void test() {
		
		assertEquals(prikA.publicKey(),  adminA.sneer().self().publicKey().mostRecent());
		
		assertEqualsUntilNow(adminA.sneer().self().publicKey().observable(), prikA.publicKey());
	}

}
