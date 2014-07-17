package sneer;

import java.text.*;
import java.util.*;


public class InteractionEvent {
	
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HH:mm");

	private final String content;
	private final Party sender;
	
	private final long timestampSent;
	private final long timestampReceived;
	
	private final boolean isOwn;
	
	
	public InteractionEvent(long timestampSent, Party sender, String content) {
		this.timestampSent = timestampSent;
		this.timestampReceived = timestampSent;
		this.sender = sender;
		this.content = content;		
		this.isOwn = true;
	}
	
	
	public InteractionEvent(long timestampSent, long timestampReceived, Party sender, String content) {
		this.timestampSent = timestampSent;
		this.timestampReceived = timestampReceived;
		this.sender = sender;
		this.content = content;
		this.isOwn = false;
	}

	
	public boolean isOwn() {
		return isOwn;
	}
	
	
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
