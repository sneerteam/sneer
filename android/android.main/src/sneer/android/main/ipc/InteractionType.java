package sneer.android.main.ipc;

import static sneer.android.main.utils.EnumUtils.names;

import java.util.Set;

public enum InteractionType {
	SESSION_PARTNER(PartnerSession.factory, true, true),
	MESSAGE(SingleMessageSession.factory, true, true),
	MESSAGE_VIEW(SingleMessageSession.factory, false, true),
	MESSAGE_COMPOSE(SingleMessageSession.factory, true, false);
	
	public final boolean canCompose;
	public final boolean canView;
	public final PluginSessionFactory factory;

	private static Set<String> names = names(values());

	InteractionType(PluginSessionFactory factory, boolean canCompose, boolean canView) {
		this.factory = factory;
		this.canCompose = canCompose;
		this.canView = canView;
	}

	public static InteractionType valueOfOrNull(String string) {
		return names.contains(string) ? valueOf(string) : null;
	}
}