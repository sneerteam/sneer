package sims.sneer.convos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import sneer.convos.Chat;
import sneer.rx.ObservedSubject;

public class ChatSim implements Chat {

    private final List<Message> messages;

    {
        messages = new ArrayList<>(3);
        messages.add(new Message(1, "Yo bro, whatsapp?", false, "Mar 23"));
        messages.add(new Message(2, "My bad, just saw your message", true, "30 mins ago"));
        messages.add(new Message(3, "Hi. Sorry, too late...", false, "15 mins ago"));
    }

    @Override
    public List<Message> messages() {
        return messages;
    }

    @Override
    public void sendMessage(String text) {
        List<Message> mess = messages;
        int size = mess.size();
        Message newMessage = new Message(size + 1, text, size % 2 == 0, size + " mins ago");
        mess.add(newMessage);
    }

    @Override
    public void setMessageRead(long id) {
        System.out.println("Message read: " + id);
    }

}
