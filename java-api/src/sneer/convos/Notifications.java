package sneer.convos;

import rx.Observable;

public interface Notifications {

	/** Emits null when there's no notification */
	Observable<Notification> get();

	public class Notification {
		public final Long convoId;
		public final String title;
		public final String text;
		public final String subText;

		public Notification(Long convoId, String title, String text, String subText) {
			this.convoId = convoId; this.title = title; this.text = text; this.subText = subText;
		}
	}

}
