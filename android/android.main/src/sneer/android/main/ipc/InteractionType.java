package sneer.android.main.ipc;

import java.util.HashSet;
import java.util.Set;

public enum InteractionType {
	SESSION_PARTNER(PartnerSession.factory, true, true),
	MESSAGE(MessageSession.factory, true, true),
	MESSAGE_VIEW(MessageSession.factory, false, true),
	MESSAGE_COMPOSE(MessageSession.factory, true, false);
	
	public final boolean canCompose;
	public final boolean canView;
	public final PluginSessionFactory factory;

	private static Set<String> set = new HashSet<String>();
	static {
		InteractionType[] e = values();
		for (InteractionType interactionType : e) {
			set.add(interactionType.name());
		}
	}

	InteractionType(PluginSessionFactory factory, boolean canCompose, boolean canView) {
		this.factory = factory;
		this.canCompose = canCompose;
		this.canView = canView;
	}

	public static InteractionType valueOfOrNull(String string) {
		return set.contains(string) ? valueOf(string) : null;
	}
}