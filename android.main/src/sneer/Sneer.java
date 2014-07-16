package sneer;

import java.util.*;

import rx.Observable;
import sneer.keys.*;
import sneer.rx.*;


public interface Sneer {

	Self self();
	
	/** All Individual contacts that you have and all Groups you are a member of. */
	Observable<Collection<Contact>> contacts();
	Contact findContact(Party party);
	void addContact(String nickname, Party party);
	
	Party produceParty(PublicKey publicKey);
	/** @return One of the following, if available, in order of priority: Nickname (if party is a Contact); "? " + party's name, if name is available; "? PUK: " + publicKey. */
	Observed<String> labelFor(Party party);

	/** All Interactions you have had. */
	Observable<Collection<Interaction>> interactions();
	/** @return an existing Interaction with party or a new one if it doesn't exist. */
	Interaction produceInteractionWith(Party party);
	
}
