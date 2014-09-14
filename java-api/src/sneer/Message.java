package sneer;

import java.text.*;
import java.util.*;

import rx.functions.*;
import sneer.commons.Comparators;
import sneer.tuples.*;


public class Message {
	
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm");

	public static final Comparator<Message> BY_TIME_RECEIVED = new Comparator<Message>() { @Override public int compare(Message e1, Message e2) {
		return Comparators.compare(e1.timestampReceived(), e2.timestampReceived());
	}};
	
	public static Func1<Tuple, Message> fromTuple(final PublicKey ownPuk) {
		return new Func1<Tuple, Message>() { @Override public Message call(Tuple tuple) {
			boolean isOwn = tuple.author().equals(ownPuk);
			return new Message((Long)tuple.get("timestampCreated"), (Long)tuple.get("timestampReceived"), tuple.payload(), isOwn);
		} };
	}

	
	private final Object content;
	
	private final long timestampCreated;
	private final long timestampReceived;
	
	private final boolean isOwn;
	
	
	public static Message createFrom(long timeCreated, long timeReceived, Object content) {
		return new Message(timeCreated, timeReceived, content, false);
	}	

	
	public static Message createOwn(long timeCreated, Object content) {
		return new Message(timeCreated, timeCreated, content, true);
	}	

	
	public Message(long timestampCreated, long timestampReceived, Object content, boolean isOwn) {
		this.timestampCreated = timestampCreated;
		this.timestampReceived = timestampReceived;
		this.content = content;
		this.isOwn = isOwn;
	}

	
	public boolean isOwn() {
		return isOwn;
	}
	
	
	public Object content() {
		return content;
	}
	

	/** When this message was created. */
	public long timestampCreated() {
		return timestampCreated;
	}

	
	/** When this message was received. */
	public long timestampReceived() {
		return timestampReceived;
	}


	public String timeCreated() {
		return SIMPLE_DATE_FORMAT.format(new Date(timestampCreated));
	}
	
	
	@Override
	public String toString() {
		return "Message [" + timestampCreated + ": " + content + "]";
	}
	
	
	public void click() {
		
	}
	
	
}
