package sneer;


import rx.Observable;

public interface Session extends ConversationItem {
	long id();
	Observable<Message> messages();
	void send(Object payload);
}

