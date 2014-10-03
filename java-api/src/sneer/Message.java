package sneer;

import sneer.tuples.Tuple;

public interface Message {

	boolean isOwn();

	Object content();

	/** When this message was created. */
	long timestampCreated();

	/** When this message was received. */
	long timestampReceived();

	String timeCreated();
	
	Tuple tuple();

}