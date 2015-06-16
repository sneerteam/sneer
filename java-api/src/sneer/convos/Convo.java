package sneer.convos;

import java.util.List;

import rx.Observable;

public class Convo {

    public final String nick;

    /** @return null if invite already accepted */
    public final String inviteCodePending;

    public final Chat chat;

    public final List<SessionSummary> sessionSummaries;


    public Convo(String nick, String inviteCodePending, Chat chat, List<SessionSummary> sessionSummaries) {
        this.nick = nick;
        this.inviteCodePending = inviteCodePending;
        this.chat = chat;
        this.sessionSummaries = sessionSummaries;
    }
}
