package sneer;

import rx.*;
import sneer.rx.*;

/** An individual or a group. See type hierarchy. */
public interface Party {

	Observed<String> publicKey();

	/** The name this Party gives itself. */
	Observable<String> name();
			
}
