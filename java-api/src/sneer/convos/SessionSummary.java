package sneer.convos;

public class SessionSummary extends SessionHandle {

    public final String title;
    public final String date;
    public final String unread;

    public SessionSummary(long id, String type, boolean isOwn, String title, String date, String unread) {
        super(id, type, isOwn);
        this.title = title; this.date = date; this.unread = unread; }

    @Override
    public String toString() {
        return "SessionSummary{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", date='" + date + '\'' +
                ", unread='" + unread + '\'' +
                '}';
    }
}
