package sneer.convos;

import sneer.flux.Action;

import static sneer.flux.Action.action;

public class ChatMessage {

   	public final long originalId;
   	public final String text;
   	public final boolean isOwn;
   	public final String date;

   	public ChatMessage(long originalId, String text, boolean isOwn, String date) { this.originalId = originalId; this.text = text; this.isOwn = isOwn; this.date = date; }

}
