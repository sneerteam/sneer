package sneer;

import sneer.tuples.Tuple;

public interface ConversationItem {
	PublicKey author();
	boolean isOwn();

	String type();

	String label();
	byte[] jpegImage();

	long timestampCreated();
	long timestampReceived();
	/** Pretty representation of the time created. */
	String timeCreated();

	long id();
	Tuple tuple();
}
