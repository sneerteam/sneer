package sims.sneer.conversations;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.subjects.BehaviorSubject;
import sneer.conversations.ConversationList;

public class ConversationListSim implements ConversationList {

	@Override
	public Observable<List<Summary>> summaries() {
		BehaviorSubject<List<Summary>> ret = BehaviorSubject.create();
		ArrayList<Summary> data = new ArrayList<>(10000);
		for (int i = 0; i < 10000; i++)
			data.add(new Summary("Wesley " + i, "Hello " + i, i + " Days Ago", unread(i), 1042+i));
		ret.onNext(data);
		return ret;
	}

	private static final String[] UNREAD_OPTIONS = {"?", "*", ""};
	private static String unread(int i) {
		return UNREAD_OPTIONS[i % 3];
	}

}