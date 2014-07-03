package sneer.snapi;

public class CloudNotification {
	
	public final Contact contact;
	public final CharSequence contentText;
	public final long timestamp;
	public final Object payload;
	
	public CloudNotification(Contact contact, CharSequence contentText, long timestamp, Object payload) {
		this.contact = contact;
		this.contentText = contentText;
		this.timestamp = timestamp;
		this.payload = payload;		
	}

}
