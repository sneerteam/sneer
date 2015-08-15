package sneer.convos;

public class SessionSummary {

    public final long id;
    public final String type;
    public final String title;
    public final String date;
    public final String unread;

    public SessionSummary(long id, String type, String title, String date, String unread) { this.id = id; this.type = type; this.title = title; this.date = date; this.unread = unread; }

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
