package sneer.core.tests;

import static org.junit.Assert.*;

import org.junit.*;

import sneer.*;
import sneer.impl.keys.*;

public class KeysTests {
	
	
	@Test
	public void recreate() {
		PrivateKey prik = Keys.createPrivateKey();
		assertEquals(prik, Keys.createPrivateKey(prik.bytes()));
		assertEquals(prik, Keys.createPrivateKey(prik.bytesAsString()));
		
		PublicKey puk = prik.publicKey();
		assertEquals(puk, Keys.createPublicKey(puk.bytes()));
		assertEquals(puk, Keys.createPublicKey(puk.bytesAsString()));
	}
	
	
}























