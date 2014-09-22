package sneer;

import rx.Observable;
import sneer.rx.Observed;

/** An individual or a group. */
public interface Party {

	Observed<PublicKey> publicKey();
	
	Observable<String> name();

}
