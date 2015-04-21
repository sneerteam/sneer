package sneer;


import java.util.List;

public interface Session extends ConversationItem {
	List<Message> messages();
	void send(Object payload);
}

