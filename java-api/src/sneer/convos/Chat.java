package sneer.convos;

import java.util.List;
import rx.Observable;

public interface Chat {

    Observable<List<Message>> messages();

    void sendMessage(String text);

    void setMessageRead(long id);

    class Message {
        public final long id;
        public final String text;
        public final boolean isOwn;
        public final long timestampCreated;

        public Message(long id, String text, boolean isOwn, long timestampCreated) {
            this.id = id;
            this.text = text;
            this.isOwn = isOwn;
            this.timestampCreated = timestampCreated;
        }
    }

}
