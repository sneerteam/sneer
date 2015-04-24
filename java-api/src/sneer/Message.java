package sneer;

public interface Message extends ConversationItem {
	/** Any parcelable object such as arrays, collections, number types, etc. Can be null. */
	Object payload();
}