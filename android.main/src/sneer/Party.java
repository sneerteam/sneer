package sneer;

import rx.*;
import sneer.keys.*;
import sneer.rx.*;

/** An individual or a group. See type hierarchy. */
public interface Party {

	Observed<PublicKey> publicKey();

	/** The name this Party gives itself. */
	Observable<String> name();
			
}
