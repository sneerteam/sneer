package sneer.impl;

import rx.*;
import sneer.*;

/** A base class for the Sneer implementation and the simulator. */
public abstract class SneerBase implements Sneer {

	@Override
	public Observable<String> nameFor(Party party) {
		return Observable.from("? PublicKey: " + party.publicKey().current()).concatWith(findContact(party).flatMap(Contact.TO_NICKNAME));
	}

}
