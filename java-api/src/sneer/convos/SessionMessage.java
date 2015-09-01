package sneer.convos;

public class SessionMessage {

	public final Object payload;
	public final boolean isOwn;

    public SessionMessage(Object payload, boolean isOwn) {
        this.payload = payload;
        this.isOwn = isOwn;
    }

    @Override
    public String toString() {
        return "SessionMessage{" +
                "payload=" + payload +
                ", isOwn=" + isOwn +
                '}';
    }

}
