package sneer;

import rx.Observable;
import sneer.commons.exceptions.FriendlyException;
import sneer.tuples.TupleSpace;

import java.util.List;

public interface Sneer {
	
	Party self();
	Profile profileFor(Party party);
	
	/** All Individual contacts that you have and all Groups you are a member of ordered alphabetically.*/
	Observable<List<Contact>> contacts();
	/** @return null if party is not a contact. */
	Contact findContact(Party party); 
	/** @throws FriendlyException if nickname is already set for another contact or if party is already a contact. */
	void addContact(String nickname, Party party) throws FriendlyException;
	/** @return null if the new nickname is ok or a reason why the new nickname is not ok. */
	String problemWithNewNickname(PublicKey publicKey, String newNick);

	
	Party produceParty(PublicKey publicKey);
	/** @return One of the following, if available, in order of priority: Nickname (if party is a Contact); "? " + party's name, if name is available; "? PUK: " + publicKey. */

	Conversations conversations();

	TupleSpace tupleSpace();

}

