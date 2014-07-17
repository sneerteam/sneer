package sneer.impl;

import rx.*;
import sneer.*;
import sneer.rx.*;

/** A base class for the Sneer implementation and the simulator. */
public abstract class SneerBase implements Sneer {

	@Override
	public Observed<String> labelFor(Party party) {
		//TODO React party becoming a new contact or being deleted as a contact. Use party name before public key, if available.
		Contact contact = findContact(party);
		Observable<String> ret = contact != null
			? contact.nickname().observable()
			: Observable.from("? PublicKey: " + party.publicKey().mostRecent());
		return new Observed<String>(ret);
	}

}
