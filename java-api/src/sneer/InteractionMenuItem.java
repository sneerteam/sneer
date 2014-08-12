package sneer;

import java.util.*;

import rx.functions.*;

public interface InteractionMenuItem extends Action0 {

	byte[] icon();
	String caption();
	
	public static final Comparator<InteractionMenuItem> BY_ALPHABETICAL_ORDER = new Comparator<InteractionMenuItem>() { @Override public int compare(InteractionMenuItem e1, InteractionMenuItem e2) {
		return e1.caption().compareTo(e2.caption());
	}};
	
}
