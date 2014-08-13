package sneer;

import java.util.*;

import rx.Observable;
import sneer.commons.exceptions.*;
import sneer.rx.*;
import sneer.tuples.*;

public interface Sneer {
	
	Party self();
	Profile profileFor(Party party);
	
	/** All Individual contacts that you have and all Groups you are a member of ordered alphabetically.*/
	Observable<List<Contact>> contacts();
	/** @return null if party is not a contact. */
	Contact findContact(Party party);
	/** @return null if nickname not found. */
	Contact findContact(String nickname);
	WritableContact writable(Contact contact);
	/** @throws FriendlyException if nickname is already set for another contact or if party is already a contact. */
	void addContact(String nickname, Party party) throws FriendlyException;
	
	
	Party produceParty(PublicKey publicKey);
	/** @return One of the following, if available, in order of priority: Nickname (if party is a Contact); "? " + party's name, if name is available; "? PUK: " + publicKey. */
	Observed<String> nameFor(Party party);

	/** All Interactions you have had, ordered by most recent first. */
	Observable<List<Interaction>> interactions();
	/** All Interactions of eventType have had, ordered by most recent first. */
	Observable<List<Interaction>> interactionsContaining(String eventType);
	/** @return an existing Interaction with party or a new one if it doesn't exist. */
	Interaction produceInteractionWith(Party party);
	
	TupleSpace tupleSpace();


}
