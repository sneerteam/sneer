package sneer;

import sneer.tuples.Tuple;

public interface Message /* extends ConversationItem */ {

	boolean isOwn();

	String messageType();
	/** Text, if present, or messageType, if present, or empty string. Never null. */
	String label();
	byte[] jpegImage();
	/** Any parcelable object such as arrays, collections, number types, etc. Can be null. */
	Object payload();


	/** When this message was created. */
	long timestampCreated();
	/** When this message was received. */
	long timestampReceived();
	/** Pretty representation of the time created. */
	String timeCreated();
	
	Tuple tuple();

}