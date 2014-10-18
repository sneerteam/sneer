package sneer;

import sneer.tuples.Tuple;

public interface Message {

	boolean isOwn();

	String messageType();
	/** Can be null. */
	String text();
	/** Not guaranteed to be a valid url. Can be null. */
	String url();
	/** Can be null. */
	byte[] jpegImage();
	/** Any parcelable object such as arrays, collections, number types, etc. Can be null. */
	Object payload();

	/** Text, if present, or url, if present, or messageType, if present, or empty string. Never null. */
	String label();

	/** When this message was created. */
	long timestampCreated();
	/** When this message was received. */
	long timestampReceived();
	/** Pretty representation of the time created. */
	String timeCreated();
	
	Tuple tuple();

}