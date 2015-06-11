package sims.sneer.convos;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.subjects.BehaviorSubject;
import sneer.commons.exceptions.FriendlyException;
import sneer.convos.Convo;
import sneer.convos.Convos;


@SuppressWarnings("unused")
public class ConvosSim implements Convos {

	@Override
	public Observable<List<Summary>> summaries() {
		BehaviorSubject<List<Summary>> ret = BehaviorSubject.create();
		ArrayList<Summary> data = new ArrayList<>(10000);
		for (int i = 0; i < 10000; i++)
			data.add(new Summary("Wesley " + i, "Hello " + i, i + " Days Ago", unread(i), 1042+i));
		ret.onNext(data);
		return ret;
	}

	@Override
	public String problemWithNewNickname(String newNick) {
		if (newNick.isEmpty()) return "cannot be empty";
		if (newNick.equals("Neide")) return "is already a contact";
		return null;
	}

	@Override
	public Observable<Long> startConvo(String newContactNick) {
		if (newContactNick.equals("Wesley"))
			return Observable.error(
					new FriendlyException("Wesley is already a contact"));
		return Observable.just(4242L);
	}

    @Override
    public Convo getById(long id) {
        return new ConvoSim();
    }

    private static final String[] UNREAD_OPTIONS = {"?", "*", ""};
	private static String unread(int i) {
		return UNREAD_OPTIONS[i % 3];
	}

}