package sneer;


import rx.Observable;

public interface Session extends ConversationItem {

	Observable<Message> messages();
	void send(Object payload);

}

