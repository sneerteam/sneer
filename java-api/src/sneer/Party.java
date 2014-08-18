package sneer;

import rx.*;
import sneer.rx.*;

/** An individual or a group. */
public interface Party {

	Observed<PublicKey> publicKey();
	
	Observable<String> name();

}
