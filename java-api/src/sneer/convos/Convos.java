package sneer.convos;

import java.util.List;

import rx.Observable;

public interface Convos {

	Observable<List<Summary>> summaries();

	/** @return null if the new nickname is ok or a reason why the new nickname is not ok (empty or already used by another Contact). */
	Observable<String> problemWithNewNickname(String newContactNick);

	/** @return The new contact id.
	 * @throws sneer.commons.exceptions.FriendlyException (via Observable) (see problemWithNewNickname(newContactNick)). */
	Observable<Long> startConvo(String newContactNick);
	/** @return The new contact id.
	 * @throws sneer.commons.exceptions.FriendlyException (via Observable) (see problemWithNewNickname(newContactNick)). */
	Observable<Long> acceptInvite(String newContactNick, String contactPuk, String inviteCodeReceived);

	Observable<Convo> getById(long contactId);

}
