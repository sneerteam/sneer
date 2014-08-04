package sneer;

import sneer.rx.*;

/** An individual or a group. */
public interface Party {

	Observed<PublicKey> publicKey();

}
