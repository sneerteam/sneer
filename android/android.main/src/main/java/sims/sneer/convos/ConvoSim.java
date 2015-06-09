package sims.sneer.convos;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import sneer.convos.Chat;
import sneer.convos.Convo;
import sneer.convos.SessionSummary;

public class ConvoSim implements Convo {

    @Override
    public Observable<String> nick() {
        return Observable.just("Nicholas");
    }

    @Override
    public Observable<String> inviteCodePending() {
        return Observable.just(null);
    }

    @Override
    public Chat chat() {
        return new ChatSim();
    }

    @Override
    public Observable<List<SessionSummary>> sessionSummaries() {
        return Observable.just((List<SessionSummary>)new ArrayList<SessionSummary>());
    }

}
