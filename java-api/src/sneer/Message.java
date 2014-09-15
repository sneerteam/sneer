package sneer;

import java.util.Comparator;

import sneer.commons.Comparators;
import sneer.tuples.Tuple;

public interface Message {

	public static final Comparator<Message> BY_TIME_RECEIVED = new Comparator<Message>() {
		@Override
		public int compare(Message e1, Message e2) {
			return Comparators.compare(e1.timestampReceived(), e2.timestampReceived());
		}
	};

	boolean isOwn();

	Object content();

	/** When this message was created. */
	long timestampCreated();

	/** When this message was received. */
	long timestampReceived();

	String timeCreated();
	
	Tuple tuple();

}