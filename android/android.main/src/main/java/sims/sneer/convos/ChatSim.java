package sims.sneer.convos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import sneer.convos.Chat;
import sneer.rx.ObservedSubject;

public class ChatSim implements Chat {

    private final ObservedSubject<List<Message>> messages;

    {
        messages = ObservedSubject.create((List<Message>)Collections.EMPTY_LIST);
        ArrayList<Message> data = new ArrayList<>(3);
        data.add(new Message(1, "Yo bro, whatsapp?", false, System.currentTimeMillis() - 60 * 60 * 1000));
        data.add(new Message(2, "My bad, just saw your message", true, System.currentTimeMillis() - 30 * 60 * 1000));
        data.add(new Message(3, "Hi. Sorry, too late...", false, System.currentTimeMillis() - 1 * 60 * 1000));
        messages.onNext(data);
    }

    @Override
    public Observable<List<Message>> messages() {
        return messages.observable();
    }

    @Override
    public void sendMessage(String text) {
        List<Message> mess = messages.current();
        int size = mess.size();
        Message newMessage = new Message(size + 1, text, size % 2 == 0, System.currentTimeMillis());
        mess.add(newMessage);
        messages.onNext(mess);
    }

    @Override
    public void setMessageRead(long id) {
        System.out.println("Message read: " + id);
    }

}
