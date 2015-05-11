package sneer.android;

public interface Message {

	boolean wasSentByMe();

	Object payload();

}
