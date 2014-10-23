package sneer.crypto.impl;

import static sneer.commons.exceptions.Exceptions.check;

import java.security.SecureRandom;

/**
 * Using SecureRandom instances provided by the java security mechanism is a
 * bloody mess. There is no way of creating a new Secure random implementation
 * with a given seed and specific algorithm.
 */
class RandomWrapper extends SecureRandom { private static final long serialVersionUID = 1L;

	private final byte[] randomBytes;
	private boolean wasUsed = false;

	RandomWrapper(byte[] randomBytes) {
		this.randomBytes = randomBytes;
	}

	@Override
	public String getAlgorithm() {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void setSeed(byte[] seed) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setSeed(long seed) {
		return; // ignore initial seed set by superclasses
	}

	@Override
	public byte[] generateSeed(int numBytes) {
		throw new UnsupportedOperationException();
	}

	@Override
	public synchronized void nextBytes(byte[] bytes) {
		check(bytes.length == randomBytes.length);
		check(!wasUsed);
		wasUsed = true;

		System.arraycopy(randomBytes, 0, bytes, 0, randomBytes.length);
	}

	
}