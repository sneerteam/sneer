package sneer;

import rx.*;


public interface Sneer {

	Self self();
	
	/** All Individual contacts that you have and all Groups you are a member of. */
	Observable<Contact> contacts();
	Contact findContact(String publicKey);
	void addContact(String nickname, Party party);
	
	/** All Individual contacts that you have and all Groups you are a member of. */
	Observable<Party> parties();
	
	Party findParty(String publicKey);

	/** All Interactions you have had. */
	Observable<Interaction> interactions();

	/** @return an existing Interaction with party or a new one if it doesn't exist. */
	Interaction produceInteractionWith(Party party);
	
}
