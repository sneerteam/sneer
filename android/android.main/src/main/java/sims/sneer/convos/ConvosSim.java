package sims.sneer.convos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
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
		return Observable.just(4242L);
	}

	@Override
	public Observable<Long> acceptInvite(String newContactNick, String contactPuk, String inviteCodeReceived) {
		System.out.println("Convos.acceptInvite: " + newContactNick + " puk: " + contactPuk + " inviteCode: " + inviteCodeReceived);
		return startConvo(newContactNick);
	}

    @Override
    public Observable<Convo> getById(long id) {
        return Observable.just(new Convo(id, "Nicholas", null, messages(), new ArrayList<SessionSummary>()));
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