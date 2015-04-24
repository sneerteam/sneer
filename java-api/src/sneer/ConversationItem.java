package sneer;

import sneer.tuples.Tuple;

public interface ConversationItem {
	boolean isOwn();
	long id();
	PublicKey author();
	String type();
	String label();
	byte[] jpegImage();

	/** When this message was created. */
	long timestampCreated();
	/** When this message was received. */
	long timestampReceived();
	/** Pretty representation of the time created. */
	String timeCreated();

	Tuple tuple();
}
