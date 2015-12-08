package sneer.convos;

public class ChatMessage {

   	public final long id;
   	public final String text;
   	public final boolean isOwn;
   	public final String date;

   	public ChatMessage(long id, String text, boolean isOwn, String date) { this.id = id; this.text = text; this.isOwn = isOwn; this.date = date; }

}
