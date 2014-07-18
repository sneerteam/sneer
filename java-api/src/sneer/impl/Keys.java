package sneer.impl;

import sneer.*;

public class Keys {

	private static int nextNumber = 1;

	public static PrivateKey newPrivateKey() {
		
		final long number = nextNumber++;
		
		return new PrivateKey() {
			
			final PublicKey publicKey = new PublicKey() {
				@Override
				public String toString() {
					return "PUK:" + number;
				}
			};
			
			@Override
			public String toString() {
				return "PRIK:" + number;
			}

			@Override
			public PublicKey publicKey() {
				return publicKey;
			}			
		};
	}

}
