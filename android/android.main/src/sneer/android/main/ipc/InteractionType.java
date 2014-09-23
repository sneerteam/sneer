package sneer.android.main.ipc;

import java.util.HashSet;
import java.util.Set;

public enum InteractionType {
	SESSION_PARTNER,
	MESSAGE,
	MESSAGE_VIEW(false, true),
	MESSAGE_COMPOSE(true, false);
	
	public final boolean canCompose;
	public final boolean canView;

	private static Set<String> set = new HashSet<String>();
	static {
		InteractionType[] e = values();
		for (InteractionType interactionType : e) {
			set.add(interactionType.name());
		}
	}

	InteractionType() {
		this(true, true);
	}
	
	InteractionType(boolean canCompose, boolean canView) {
		this.canCompose = canCompose;
		this.canView = canView;
	}

	public static InteractionType valueOfOrNull(String string) {
		return set.contains(string) ? valueOf(string) : null;
	}
}