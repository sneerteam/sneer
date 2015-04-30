package sneer.android.flux2;

import java.util.List;

public interface Conversations {

	Producer<List<Conversations.Summary>> summaries();

	class Summary {
		public final String party;
		public final String textPreview;
		public final String date;
		public final String unread;
		public final long id;

		public Summary(String party, String textPreview, String date, String unread, long id) {
			this.party       = party;
			this.textPreview = textPreview;
			this.date        = date;
			this.unread      = unread;
			this.id          = id;
		}
	}

	class Click {
		public final long id;

		public Click(long id) {
			this.id = id;
		}
	}

}
