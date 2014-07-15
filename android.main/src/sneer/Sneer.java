package sneer;

import java.util.*;

import rx.Observable;
import sneer.rx.*;


public interface Sneer {

	Self self();
	
	/** All Individual contacts that you have and all Groups you are a member of. */
	Observable<Set<Contact>> contacts();
	Contact findContact(String publicKey);
	void addContact(String nickname, Party party);
	
	Party produceParty(String publicKey);
	/** @return One of the following, if available, in order of priority: Nickname (if party is a Contact); "? " + party's name, if name is available; "? PUK: " + publicKey. */
	Observed<String> labelFor(Party party);

	/** All Interactions you have had. */
	Observable<Set<Interaction>> interactions();

	/** @return an existing Interaction with party or a new one if it doesn't exist. */
	Interaction produceInteractionWith(Party party);
	
	
}
