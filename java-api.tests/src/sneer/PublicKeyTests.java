package sneer;

import static org.junit.Assert.*;

import org.junit.*;

import sneer.impl.keys.*;

public class PublicKeyTests {
	
	
	@Test
	public void recreate() {
		PublicKey puk = Keys.createPrivateKey().publicKey();
		assertEquals(puk, Keys.createPublicKey(puk.asBitcoinAddress()));
	}
	
	
}























