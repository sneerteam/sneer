package sneer;


import rx.Observable;

public interface Session extends ConversationItem {

	Observable<MessageOrUpToDate> messages();
	void send(Object payload);

	public class MessageOrUpToDate {

		private final Message message;

		public MessageOrUpToDate(Message message) {
			this.message = message;
		}

		public boolean isUpToDate() {
			return message == null;
		}

		public Message message() {
			if (message == null) throw new IllegalStateException("Message was null");
			return message;
		}

		@Override
		public String toString() {
			return isUpToDate() ? "UP TO DATE" : message.toString();
		}
	}

	public static final MessageOrUpToDate UP_TO_DATE = new MessageOrUpToDate(null);
}

