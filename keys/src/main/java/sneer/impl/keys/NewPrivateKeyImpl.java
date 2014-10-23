package sneer.impl.keys;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;
import static sneer.commons.Codec.fromHex;
import static sneer.commons.Codec.toHex;
import static sneer.commons.exceptions.Exceptions.check;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;

import sneer.PrivateKey;
import sneer.PublicKey;
import sneer.commons.Codec;
import sneer.commons.SystemReport;
import sneer.commons.exceptions.NotImplementedYet;

class NewPrivateKeyImpl implements PrivateKey { private static final long serialVersionUID = 1L;

	private final byte[] seed;

	private final PublicKey puk;
	private final java.security.PrivateKey delegatePrik;
	
	
	NewPrivateKeyImpl() {
		this(randomSeed());
	}

	
	NewPrivateKeyImpl(String bytesAsString) {
		this(fromHex(bytesAsString));
	}

	
	NewPrivateKeyImpl(byte[] seed) {
		check(seed.length == 32);
		this.seed = seed;
	
		throw new NotImplementedYet();
		
//		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
//	    ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256k1");
//	    keyGen.initialize(ecSpec, new RandomWrapper(seed));
//	    KeyPair pair = keyGen.generateKeyPair();
//		
//		delegatePrik = pair.getPrivate();
//		puk = pair.getPublic().;
	}
	
	
	@Override
	public PublicKey publicKey() {
		return puk;
	}
	
	
	@Override
	public byte[] bytes() {
		return seed;
	}
	
	
	@Override
	public String bytesAsString() {
		return toHex(bytes());
	}

	
	@Override
	public String toString() {
		return "PRIK:" + publicKey().bytesAsString().substring(0, 5);
	}
	
	
	private static SecureRandom random = new SecureRandom();
	private static byte[] randomSeed() {
		random.setSeed(urandomBytes());
		random.setSeed(currentTimeMillis());
		random.setSeed(nanoTime());
		random.setSeed(new Object().hashCode());
		
		byte[] ret = new byte[32];
		random.nextBytes(ret);
		return ret;
	}

	
	private static byte[] urandomBytes() {
		byte[] ret = new byte[32];
		try {
			DataInputStream in = new DataInputStream(new FileInputStream("/dev/urandom"));
			in.readFully(ret);
			in.close();
			SystemReport.updateReport("security/random-seed", Arrays.toString(ret)); int removeThisLine;
		} catch (Exception e) {
			SystemReport.updateReport("security/random-seed/warning", "Warning: Unable to read from /dev/urandom");
		}
		return ret;
	}


	@Override
	public int hashCode() {
		return Codec.hashCode(seed);
	}

	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof NewPrivateKeyImpl))
			return false;
		NewPrivateKeyImpl other = (NewPrivateKeyImpl) obj;
		return Arrays.equals(seed, other.seed);
	}

}