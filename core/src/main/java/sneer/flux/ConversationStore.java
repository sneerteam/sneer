package sneer.flux;

import java.util.List;

import rx.Observable;

public interface ConversationStore { //TODO Flux: Rename from ConversationStore to ConversationSource.

	Observable<List<Summary>> summaries();

	public static class Summary { //TODO Flux: Rename from ConversationStore.Summary to ConversationSource.Summary.
		public final String party;
		public final String textPreview;
		public final long timestamp;
		public final long unread;

		public Summary(String party, String textPreview, long timestamp, long unread) {
			this.party = party;
			this.textPreview = textPreview;
			this.timestamp = timestamp;
			this.unread = unread;
		}
	}

}
