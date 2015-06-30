package sims.sneer.convos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import sneer.commons.exceptions.FriendlyException;
import sneer.convos.Convo;
import sneer.convos.Convos;
import sneer.convos.SessionSummary;

import static java.util.concurrent.TimeUnit.SECONDS;

COnvosAc
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
    public Observable<Convo> getById(long id) {
        String inviteCodePending = id % 2 == 1 ? null : "InviteCode" + id;
        return inviteCodePending == null
            ? Observable.just(new Convo("Nicholas", null, new ChatSim(), new ArrayList<SessionSummary>()))
//            : Observable.just(new Convo("Nicholas", inviteCodePending, new ChatSim(), new ArrayList<SessionSummary>()));
            : Observable.just(new Convo("Nicholas", inviteCodePending, new ChatSim(), new ArrayList<SessionSummary>())).concatWith(
                Observable.timer(3, SECONDS, AndroidSchedulers.mainThread()).map(new Func1<Long, Convo>() {
                    @Override
                    public Convo call(Long aLong) {
                        return new Convo("Nicholas", null, new ChatSim(), new ArrayList<SessionSummary>());
                    }
                }));
    }

    private static final String[] UNREAD_OPTIONS = {"?", "*", ""};
	private static String unread(int i) {
		return UNREAD_OPTIONS[i % 3];
	}

}