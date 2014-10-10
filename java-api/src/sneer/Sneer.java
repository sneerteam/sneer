package sneer;

import java.util.List;

import rx.Observable;
import sneer.commons.exceptions.FriendlyException;
import sneer.tuples.TupleSpace;

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
	String problemWithNewNickname(String newNick);

	
	Party produceParty(PublicKey publicKey);
	/** @return One of the following, if available, in order of priority: Nickname (if party is a Contact); "? " + party's name, if name is available; "? PUK: " + publicKey. */

	/** All Conversations you have had, ordered by most recent first. */
	Observable<List<Conversation>> conversations();
	/** All Conversations of messageType have had, ordered by most recent first. */
	Observable<List<Conversation>> conversationsContaining(String messageType);
	/** @return an existing Conversation with party or a new one if it doesn't exist. */
	Conversation produceConversationWith(Party party);
	
	void setConversationMenuItems(List<ConversationMenuItem> menuItems);
	
	TupleSpace tupleSpace();

}
