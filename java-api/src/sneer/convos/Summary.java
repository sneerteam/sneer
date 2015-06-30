package sneer.convos;

import java.io.Serializable;

public class Summary implements Serializable { private static final long serialVersionUID = 1;

	public final String nickname;
	public final String textPreview;
	public final String date;
	public final String unread;
	public final long convoId;

	public Summary(String nickname, String textPreview, String date, String unread, long convoId) { this.nickname = nickname; this.textPreview = textPreview; this.date = date; this.unread = unread; this.convoId = convoId; }

}
