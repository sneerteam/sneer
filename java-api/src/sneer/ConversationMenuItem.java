package sneer;

import rx.functions.Action1;

import java.util.Comparator;

public interface ConversationMenuItem extends Action1<PublicKey> {

	byte[] icon();
	String caption();
	
	public static final Comparator<ConversationMenuItem> BY_ALPHABETICAL_ORDER = new Comparator<ConversationMenuItem>() { @Override public int compare(ConversationMenuItem e1, ConversationMenuItem e2) {
		return e1.caption().compareTo(e2.caption());
	}};
	
}
