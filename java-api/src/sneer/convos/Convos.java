package sneer.convos;

import java.io.Serializable;
import java.util.List;

import rx.Observable;
import sneer.commons.exceptions.FriendlyException;

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

	Observable<Convo> getById(long id);

}
