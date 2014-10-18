package sneer;

import java.text.*;
import java.util.*;

import rx.functions.*;
import sneer.tuples.*;

public class MessageImpl implements Message {
	
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm");

	public static Func1<Tuple, Message> fromTuple(final PublicKey ownPuk) {
		return new Func1<Tuple, Message>() { @Override public Message call(Tuple tuple) {
			boolean isOwn = tuple.author().equals(ownPuk);
			return new MessageImpl((Long)tuple.get("timestampCreated"), tuple.payload(), isOwn);
		} };
	}

	
	private final Object payload;
	
	private final long timestampCreated;
	
	private final boolean isOwn;
	
	
	public static Message createFrom(long timeCreated, Object content) {
		return new MessageImpl(timeCreated, content, false);
	}	

	
	public static Message createOwn(long timeCreated, Object content) {
		return new MessageImpl(timeCreated, content, true);
	}	

	
	public MessageImpl(long timestampCreated, Object content, boolean isOwn) {
		this.timestampCreated = timestampCreated;
		this.payload = content;
		this.isOwn = isOwn;
	}

	
	@Override
	public boolean isOwn() {
		return isOwn;
	}
	
	
	@Override
	public Object payload() {
		return payload;
	}
	

	/** When this message was created. */
	@Override
	public long timestampCreated() {
		return timestampCreated;
	}

	
	/** When this message was received. */
	@Override
	public long timestampReceived() {
		return 0;
	}


	@Override
	public String timeCreated() {
		return SIMPLE_DATE_FORMAT.format(new Date(timestampCreated));
	}
	
	
	@Override
	public String toString() {
		return "Message [" + timestampCreated + ": " + payload + "]";
	}


	@Override
	public Tuple tuple() {
		return null;
	}


	@Override
	public String messageType() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String text() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String url() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public byte[] jpegImage() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String label() {
		if (text()        != null) return text();
		if (url()         != null) return url();
		if (messageType() != null) return messageType();
		return "";
	}
	
}
