package sneer.convos;

import java.util.List;

import rx.Observable;
import sneer.Message;

public interface Chat {

    Observable<List<Message>> messages();

    void sendMessage(String text);

    void setMessageRead(long id);

}
