package sneer.convos;

import java.util.List;
import sneer.flux.Action;
import sneer.flux.Request;

import static sneer.flux.Action.action;
import static sneer.flux.Request.request;

public class Convo {

	public static Action sendMessage(long id, String text) { return action("send-message", "contact-id", id, "text", text);}

	public final String nickname;
	/** null if invite already accepted */
	public final String inviteCodePending;
	public final List<ChatMessage> messages;
	public final List<SessionSummary> sessionSummaries;
	public final long id;

	public Convo(long id, String nickname, String inviteCodePending, List<ChatMessage> messages, List<SessionSummary> sessionSummaries) { this.id = id; this.nickname = nickname; this.inviteCodePending = inviteCodePending; this.messages = messages; this.sessionSummaries = sessionSummaries; }

	public Action setNickname(String newNick) { return action("set-nickname", "contact-id", id, "new-nick", newNick); }
	public Action sendMessage(String text) { return sendMessage(id, text); }
	public Action setRead(ChatMessage message) { return action("set-message-read", "contact-id", id, "message-id", message.originalId); }

	@Override
	public String toString() {
		return "Convo{" +
				"nickname='" + nickname + '\'' +
				", inviteCodePending='" + inviteCodePending + '\'' +
				", messages=" + messages +
				", sessionSummaries=" + sessionSummaries +
				", id=" + id +
				'}';
	}

}
