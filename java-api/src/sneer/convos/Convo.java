package sneer.convos;

import java.util.List;

import rx.Observable;
import sneer.Session;

public interface Convo {

    Observable<String> nick();

    /** @return null if invite already accepted */
    Observable<String> inviteCodePending();

    Chat chat();

    Observable<List<Session>> sessions();
    long startSession(String type);

}
