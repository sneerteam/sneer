package sneer.convos;

import java.util.List;

import rx.Observable;

public interface Convos {

	Observable<List<Summary>> summaries();

	/** Emits null if the new nickname is ok or a reason why the new nickname is not ok (empty or already used by another Contact). */
	Observable<String> problemWithNewNickname(String newNickname);

	/** Emits the new Convo id.
	 * @throws sneer.commons.exceptions.FriendlyException (via Observable) (see problemWithNewNickname(newContactNick)). */
	Observable<Long> startConvo(String newContactNickname);

	/** Emits the new Convo id.
	 * @throws sneer.commons.exceptions.FriendlyException (via Observable) (see problemWithNewNickname(newContactNick)). */
	Observable<Long> acceptInvite(String newContactNickname, String inviterPuk, String inviteCodeReceived);

	Observable<Convo> getById(long convoId);

	/** Emits null if the user has not yet accepted the invite */
	Observable<Long> findConvo(String inviterPuk);

	String ownPuk();

}
