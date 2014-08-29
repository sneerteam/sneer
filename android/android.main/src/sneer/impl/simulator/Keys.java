package sneer.impl.simulator;

import java.util.concurrent.atomic.*;

import sneer.*;

public class Keys {
	
	private static final AtomicInteger counter = new AtomicInteger();

	public static PrivateKey createPrivateKey() {
		
		final int key = counter.incrementAndGet();
		
		final PublicKey puk = createPublicKey("PUK-" + key);
		
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
				return puk;
			}
			
			@Override
			public boolean equals(Object o) {
				return o instanceof PrivateKey
					&& ((PrivateKey)o).bytesAsString().equals(bytesAsString());
			}
			
			@Override
			public int hashCode() {
				return bytesAsString().hashCode();
			}};
	}

	public static PublicKey createPublicKey(final String bytesAsString) {
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
			}};
	}

}
