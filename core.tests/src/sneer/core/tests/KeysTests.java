package sneer.core.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.impl.keys.KeysImpl;

public class KeysTests {
	
	
	@Test
	public void recreate() {
		KeysImpl subject = new KeysImpl();
		
		PrivateKey prik = subject.createPrivateKey();
		assertEquals(prik, subject.createPrivateKey(prik.bytes()));
		assertEquals(prik, subject.createPrivateKey(prik.bytesAsString()));
		
		PublicKey puk = prik.publicKey();
		assertEquals(puk, subject.createPublicKey(puk.bytes()));
		assertEquals(puk, subject.createPublicKey(puk.bytesAsString()));
	}
	
	
}























