package sneer.android.ipcold;

import java.util.Set;

import static sneer.android.utils.EnumUtils.names;

public enum PluginType {
	PARTNER_SESSION(PartnerSessionFactory.singleton, true, true),
	MESSAGE(SingleMessageSessionFactory.singleton, true, true),
	MESSAGE_VIEW(SingleMessageSessionFactory.singleton, false, true),
	MESSAGE_COMPOSE(SingleMessageSessionFactory.singleton, true, false),

	// future
	GROUP_SESSION(null, true, true);

	public final boolean canCompose;
	public final boolean canView;
	public final PluginSessionFactory factory;

	private static Set<String> names = names(values());

	PluginType(PluginSessionFactory factory, boolean canCompose, boolean canView) {
		this.factory = factory;
		this.canCompose = canCompose;
		this.canView = canView;
	}


	public static PluginType valueOfOrNull(String string) {
		return names.contains(string) ? valueOf(string) : null;
	}

}