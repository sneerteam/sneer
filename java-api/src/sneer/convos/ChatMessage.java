package sneer.convos;

public class ChatMessage {

	public enum DeliveryStatus { WAITING, SENT, RECEIVED, READ };

   	public final long id;
   	public final String text;
   	public final boolean isOwn;
   	public final String date;
   	public final DeliveryStatus delivery;

   	public ChatMessage(long id, String text, boolean isOwn, String date, DeliveryStatus delivery) { this.id = id; this.text = text; this.isOwn = isOwn; this.date = date; this.delivery = delivery; }

}
