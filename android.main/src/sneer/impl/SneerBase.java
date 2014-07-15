package sneer.impl;

import rx.subjects.*;
import sneer.*;
import sneer.rx.*;

/** A base class for the Sneer implementation and the simulator. */
public abstract class SneerBase implements Sneer {

	@Override
	public Observed<String> labelFor(Party party) {
		BehaviorSubject<String> subject = BehaviorSubject.create("? PublicKey: " + party.publicKey().mostRecent());
		...
		return new Observed<String>(subject);
	}

}
