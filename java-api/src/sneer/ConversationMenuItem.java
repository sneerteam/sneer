package sneer;

import java.util.*;

import rx.functions.*;

public interface ConversationMenuItem extends Action0 {

	byte[] icon();
	String caption();
	
	public static final Comparator<ConversationMenuItem> BY_ALPHABETICAL_ORDER = new Comparator<ConversationMenuItem>() { @Override public int compare(ConversationMenuItem e1, ConversationMenuItem e2) {
		return e1.caption().compareTo(e2.caption());
	}};
	
}
