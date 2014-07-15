package sneer;

import rx.Observable;

/** An individual or a group. See type hierarchy. */
public interface Party {

	Observable<String> publicKey();

	/** The name this Party gives itself. */
	Observable<String> name();
		
}
