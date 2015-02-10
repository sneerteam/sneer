package sneer.impl.simulator;

import sneer.PrivateKey;
import sneer.PublicKey;

import java.util.concurrent.atomic.AtomicInteger;

public class KeysSimulator {
	
	private static final AtomicInteger counter = new AtomicInteger();

	static PrivateKey createPrivateKey() {
		
		final int key = counter.incrementAndGet();
		
		
		return new PrivateKey() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public byte[] toBytes() {
				return toHex().getBytes();
			}

			@Override
			public String toHex() {
				return "PRIK-" + key;
			}

			@Override
			public PublicKey publicKey() {
				return createPublicKey("PUK-" + key);
			}
			
			@Override
			public boolean equals(Object o) {
				return o instanceof PrivateKey
					&& ((PrivateKey)o).toHex().equals(toHex());
			}
			
			@Override
			public int hashCode() {
				return toHex().hashCode();
			}
			
			@Override
			public String toString() {
				return toHex();
			}
		};
	}

	
	static PublicKey createPublicKey(final String bytesAsString) {
	
		return new PublicKey() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public byte[] toBytes() {
				return toHex().getBytes();
			}

			@Override
			public String toHex() {
				return bytesAsString;
			}
			
			@Override
			public boolean equals(Object o) {
				return o instanceof PublicKey
					&& ((PublicKey)o).toHex().equals(toHex());
			}
			
			@Override
			public int hashCode() {
				return toHex().hashCode();
			}

			@Override
			public String toString() {
				return toHex();
			}
		};
	}

}
