package sneer.convos;

import java.util.List;

import rx.Observable;
import sneer.flux.Action;

import static sneer.flux.Action.action;

public class Convo {

    public final String nickname;
    /** null if invite already accepted */
    public final String inviteCodePending;
    public final Chat chat;
    public final List<SessionSummary> sessionSummaries;
    private final long id;

    public Convo(long id, String nickname, String inviteCodePending, Chat chat, List<SessionSummary> sessionSummaries) {
        this.id = id;
        this.nickname = nickname;
        this.inviteCodePending = inviteCodePending;
        this.chat = chat;
        this.sessionSummaries = sessionSummaries;
    }

    public Action setNickname(String newNick) {
        return action("set-nickname", "convo-id", id, "new-nick", newNick);
    }
}
