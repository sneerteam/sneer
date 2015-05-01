package sneer.android.flux2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.subjects.BehaviorSubject;

public interface Conversations {

	Observable<List<Summary>> summaries();

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


	class Sim implements Conversations {

		@Override
		public Observable<List<Summary>> summaries() {
			BehaviorSubject<List<Summary>> subject = BehaviorSubject.create();
			ArrayList<Summary> data = new ArrayList<>(10000);
			for (int i = 0; i < 10000; i++)
				data.add(new Summary("Wesley " + i, "Hello " + i, i + " Days Ago", ""+i, 1042+i));
			subject.onNext(data);
			return subject;
		}
	}

}
