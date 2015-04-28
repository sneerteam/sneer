package sneer.android;

public interface Message {
	boolean isOwn();
	Object payload();
}
