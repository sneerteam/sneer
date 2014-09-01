package sneer.core.tests;

import static org.junit.Assert.*;

import org.junit.*;

import sneer.*;
import sneer.impl.keys.*;

public class KeysTests {
	
	
	@Test
	public void recreate() {
		PrivateKey prik = KeysImpl.createPrivateKey();
		assertEquals(prik, KeysImpl.createPrivateKey(prik.bytes()));
		assertEquals(prik, KeysImpl.createPrivateKey(prik.bytesAsString()));
		
		PublicKey puk = prik.publicKey();
		assertEquals(puk, KeysImpl.createPublicKey(puk.bytes()));
		assertEquals(puk, KeysImpl.createPublicKey(puk.bytesAsString()));
	}
	
	
}























