package sneer.impl.simulator;

import java.util.concurrent.atomic.AtomicInteger;

import sneer.PrivateKey;
import sneer.PublicKey;

public class KeysSimulator {
	
	private static final AtomicInteger counter = new AtomicInteger();

	static PrivateKey createPrivateKey() {
		
		final int key = counter.incrementAndGet();
		
		
		return new PrivateKey() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public byte[] bytes() {
				return bytesAsString().getBytes();
			}

			@Override
			public String bytesAsString() {
				return "PRIK-" + key;
			}

			@Override
			public PublicKey publicKey() {
				return createPublicKey("PUK-" + key);
			}
			
			@Override
			public boolean equals(Object o) {
				return o instanceof PrivateKey
					&& ((PrivateKey)o).bytesAsString().equals(bytesAsString());
			}
			
			@Override
			public int hashCode() {
				return bytesAsString().hashCode();
			}
			
			@Override
			public String toString() {
				return bytesAsString();
			}
		};
	}

	
	static PublicKey createPublicKey(final String bytesAsString) {
	
		return new PublicKey() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public byte[] bytes() {
				return bytesAsString().getBytes();
			}

			@Override
			public String bytesAsString() {
				return bytesAsString;
			}
			
			@Override
			public boolean equals(Object o) {
				return o instanceof PublicKey
					&& ((PublicKey)o).bytesAsString().equals(bytesAsString());
			}
			
			@Override
			public int hashCode() {
				return bytesAsString().hashCode();
			}

			@Override
			public String toString() {
				return bytesAsString();
			}
		};
	}

}
