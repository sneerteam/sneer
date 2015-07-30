package sims.sneer.convos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import sneer.commons.exceptions.FriendlyException;
import sneer.convos.ChatMessage;
import sneer.convos.Convo;
import sneer.convos.Convos;
import sneer.convos.SessionSummary;
import sneer.convos.Summary;


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
	public Observable<String> problemWithNewNickname(String newNick) {
		String ret = null;
		if (newNick.isEmpty()) ret = "cannot be empty";
		if (newNick.equals("Neide")) ret = "is already a contact";
		return Observable.just(ret);
	}

	@Override
	public Observable<Long> startConvo(String newContactNick) {
		if (newContactNick.equals("Wesley"))
			return Observable.error(
					new FriendlyException("Wesley is already a contact"));
		return Observable.just(4241L);
	}

	@Override
	public Observable<Long> acceptInvite(String newContactNick, String contactPuk, String inviteCodeReceived) {
		System.out.println("Convos.acceptInvite: " + newContactNick + " puk: " + contactPuk + " inviteCode: " + inviteCodeReceived);
		return startConvo(newContactNick);
	}

    @Override
    public Observable<Convo> getById(final long id) {
        return id % 2 == 0
           ? Observable.just(new Convo(id, "Wesley " + id, null, messages(), newSessionSummaries()))
           : Observable.concat(
                Observable.just(new Convo(id, "Pending " + id, "INVITE_CODE", Collections.<ChatMessage>emptyList(), Collections.<SessionSummary>emptyList())),
                Observable.timer(3, TimeUnit.SECONDS).map(new Func1<Long, Convo>() {
                    @Override
                    public Convo call(Long aLong) {
                        return new Convo(id, "Wesley " + id, null, messages(), newSessionSummaries());
                    }
                })
        );

    }

	private List<SessionSummary> newSessionSummaries() {
		SessionSummary[] sums = new SessionSummary[]{
			new SessionSummary(1, "chess", "Chess game, your turn", "6 minutes ago", "*"),
			new SessionSummary(2, "shopping", "Shopping List", "1 hour ago", "*"),
			new SessionSummary(3, "torogo", "ToroGo game (you won)", "3 days ago", ""),
			new SessionSummary(4, "snitcoin", "SnitCoin transfer received", "1 week ago", "")
		};
		return Arrays.asList(sums);
	}

	private int findConvoCount = 0;
    @Override
    public Observable<Long> findConvo(String inviterPuk) {
        findConvoCount++;
        return findConvoCount % 2 == 0
                        ? Observable.just(4242L)
                        : Observable.just((Long) null);
    }

    @Override
    public String ownPuk() {
        return "BABACABABACABABACABABACABABACA00BABACABABACABABACABABACABABACA00";
    }

    private List<ChatMessage> messages() {
		return Arrays.asList(
			new ChatMessage(1, "Yo bro, sup?", false, "Mar 23"),
			new ChatMessage(2, "My bad, just saw your message", true, "30 mins ago"),
			new ChatMessage(3, "Hi. Sorry, too late...", false, "15 mins ago"));
	}

	private static final String[] UNREAD_OPTIONS = {"?", "*", ""};
	private static String unread(int i) {
		return UNREAD_OPTIONS[i % 3];
	}

}
