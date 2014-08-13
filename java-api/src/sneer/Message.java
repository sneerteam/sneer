package sneer;

import java.text.*;
import java.util.*;

import sneer.commons.*;


public class Message {
	
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm");

	public static final Comparator<Message> BY_TIME_RECEIVED = new Comparator<Message>() { @Override public int compare(Message e1, Message e2) {
		return Comparators.compare(e1.timestampReceived(), e2.timestampReceived());
	}};
	
	
	private final String content;
	
	private final long timestampSent;
	private final long timestampReceived;
	
	private final boolean isOwn;
	
	
	public static Message createFrom(long timeSent, long timeReceived, String content) {
		return new Message(timeSent, timeReceived, content, false);
	}	

	
	public static Message createOwn(long timeSent, String content) {
		return new Message(timeSent, timeSent, content, true);
	}	

	
	private Message(long timestampSent, long timestampReceived, String content, boolean isOwn) {
		this.timestampSent = timestampSent;
		this.timestampReceived = timestampReceived;
		this.content = content;
		this.isOwn = isOwn;
	}

	
	public boolean isOwn() {
		return isOwn;
	}
	
	
	public String content() {
		return content;
	}
	

	/** When this message was sent. */
	public long timestampSent() {
		return timestampSent;
	}

	
	/** When this message was received. */
	public long timestampReceived() {
		return timestampReceived;
	}


	public String timeSent() {
		return SIMPLE_DATE_FORMAT.format(new Date(timestampSent));
	}
	
	
	@Override
	public String toString() {
		return "Message [" + timestampSent + ": " + content + "]";
	}

	
}
