package sneer.convos;

import java.io.Serializable;
import java.util.List;

import rx.Observable;
import sneer.flux.Request;

public interface Convos {

	class Summary implements Serializable { private static final long serialVersionUID = 1;
		public final String nickname;
		public final String textPreview;
		public final String date;
		public final String unread;
		public final long convoId;

		public Summary(String nickname, String textPreview, String date, String unread, long convoId) { this.nickname = nickname; this.textPreview = textPreview; this.date = date; this.unread = unread; this.convoId = convoId; }
	}

	Observable<List<Summary>> summaries();

	/** @return null if the new nickname is ok or a reason why the new nickname is not ok (empty or already used by another Contact). */
	String problemWithNewNickname(String newContactNick);

	/** @return The new convo id.
	 * @throws (via rx) sneer.commons.exceptions.FriendlyException (see problemWithNewNickname(newContactNick)). */
	Observable<Long> startConvo(String newContactNick);
	Observable<Long> startConvo(String newContactNick, String contactPuk, String inviteCodeReceived);

	Observable<Convo> getById(long id);

	class Actions {
		/** @return A request for the convo id of a new convo. */
		public static Request<Long> acceptInvite(String newContactNick, String contactPuk, String inviteCodeReceived) {
			return Request.request("accept-invite", "new-contact-nick", newContactNick, "contact-puk", contactPuk, "invite-code-received", inviteCodeReceived);
		}
	}

}
