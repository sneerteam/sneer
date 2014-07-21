package sneer;

import java.text.*;
import java.util.*;

import sneer.commons.*;


public class InteractionEvent {
	
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm");

	public static final Comparator<InteractionEvent> BY_TIME_RECEIVED = new Comparator<InteractionEvent>() { @Override public int compare(InteractionEvent e1, InteractionEvent e2) {
		return Comparators.compare(e1.timestampReceived(), e2.timestampReceived());
	}};

	
	private final String content;
	private final Party sender;
	
	private final long timestampSent;
	private final long timestampReceived;
	
	private final boolean isOwn;
	
	
	public static InteractionEvent createFrom(Party sender, long timeSent, long timeReceived, String content) {
		return new InteractionEvent(sender, timeSent, timeReceived, content, false);
	}	

	
	public static InteractionEvent createOwn(long timeSent, String content) {
		return new InteractionEvent(null, timeSent, timeSent, content, true);
	}	

	
	private InteractionEvent(Party sender, long timestampSent, long timestampReceived, String content, boolean isOwn) {
		this.timestampSent = timestampSent;
		this.timestampReceived = timestampReceived;
		this.sender = sender;
		this.content = content;
		this.isOwn = isOwn;
	}

	
	public boolean isOwn() {
		return isOwn;
	}
	
	
	/** @return The Party that sent this InteractionEvent or null if isOwn(). */
	public Party sender() {
		return sender;
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
		return "InteractionEvent [" + timestampSent + " " + sender + ": " + content + "]";
	}

	
}
