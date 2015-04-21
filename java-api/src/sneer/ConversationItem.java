package sneer;

public interface ConversationItem {
	//	PublicKey author();

	String type();
	String label();
	byte[] jpegImage();
	long timestamp();
}
