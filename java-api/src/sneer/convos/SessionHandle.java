package sneer.convos;


public class SessionHandle {

	public final long id;
	public final String type;
	public final boolean isOwn;

	public SessionHandle(long id, String type, boolean isOwn) {
		this.type = type;
		this.id = id;
		this.isOwn = isOwn;
	}

	@Override
	public String toString() {
		return "SessionHandle{" +
				"id=" + id +
				", type='" + type + '\'' +
				'}';
	}
}
