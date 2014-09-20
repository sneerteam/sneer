package sneer;

import rx.*;
import sneer.rx.*;

/** An individual or a group. */
public interface Party {

	Observed<PublicKey> publicKey();
	
	@Deprecated //Change return type to Observed.
	Observable<String> name();

}
