package sneer.flux;

import java.util.List;

import rx.Observable;
import sneer.admin.SneerAdmin;

/**
 * Intermediates the creation of clojure instances.
 */
public class ConversationStore {

	public static Interface createInstance(SneerAdmin admin) {
		try {
			return ((Interface) Class.forName("sneer.flux.ConversationStoreServiceProvider")
									 .getConstructor(SneerAdmin.class)
									 .newInstance(admin));
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public interface Interface {
		Observable<List<Summary>> summaries();
	}

	public static class Summary {
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
