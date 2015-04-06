package sneer;

import java.util.List;

import rx.Observable;
import sneer.commons.exceptions.FriendlyException;
import sneer.tuples.TupleSpace;

public interface Sneer {

	Party self();

	Party produceParty(PublicKey publicKey);

	Profile profileFor(Party party);

	// INVITES-TODO: Remove this:
	/** @return a pseudorandomly generated long. */
	@Deprecated
	long generateContactInvite();

	/** All Individual contacts that you have and all Groups you are a member of ordered alphabetically.*/
	Observable<List<Contact>> contacts();
	/** @return null if party is not a contact. */
	Contact findContact(Party party);

	// INVITES-TODO: Change this to problemWithNewNickname(Party party, String newNick);
	// INVITES-TODO: When the user is trying to associate a Party with a Contact and types the nickname of an existing Contact without a party, change the UI to use the existing Contact, instead of displaying the error tooltip.
	/** @return null if the new nickname is ok or a reason why the new nickname is not ok (empty or already used by another Contact). */
	String problemWithNewNickname(PublicKey publicKey, String newNick);

	// INVITES-TODO: Add a String inviteCodeReceived param: @param inviteCodeReceived The code used by the contact to add us back automatically. Can be null if we didn't receive a code to add this contact.
	/** @party Can be null if we are inviting someone and do not have their puk yet.
	 * @throws FriendlyException if there is a problemWithNewNickname(nickname) or if party is already a contact. */
	void addContact(String nickname, Party party) throws FriendlyException;

	// INVITES-TODO: Remove this:
    /** @throws FriendlyException if nickname is already set for another contact */
    @Deprecated
    void addContactWithoutParty(String nickname, long inviteCode) throws FriendlyException;

	Conversations conversations();

	TupleSpace tupleSpace();
}

