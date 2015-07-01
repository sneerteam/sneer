package sneer.convos;

import java.util.List;

import sneer.flux.Action;

import static sneer.flux.Action.action;

public class Convo {

	public final String nickname;
	/** null if invite already accepted */
    public final String inviteCodePending;

	public final List<ChatMessage> messages;
    public final List<SessionSummary> sessionSummaries;

    private final long contactId;

    public Convo(long contactId, String nickname, String inviteCodePending, List<ChatMessage> messages, List<SessionSummary> sessionSummaries) { this.contactId = contactId; this.nickname = nickname; this.inviteCodePending = inviteCodePending; this.messages = messages; this.sessionSummaries = sessionSummaries; }

	public Action setNickname(String newNick) { return action("set-nickname", "contact-id", contactId, "new-nick", newNick); }
	public Action sendMessage(String text) { return action("send-message", "contact-id", contactId, "text", text); }

}
