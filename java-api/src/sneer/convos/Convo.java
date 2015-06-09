package sneer.convos;

import java.util.List;

import rx.Observable;

public interface Convo {

    Observable<String> nick();

    /** @return null if invite already accepted */
    Observable<String> inviteCodePending();

    Chat chat();

    Observable<List<SessionSummary>> sessionSummaries();

}
