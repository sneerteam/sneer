package sneer.android.main.ipc;

public enum InteractionType {
	SESSION_PARTNER,
	MESSAGE,
	MESSAGE_VIEW(false, true),
	MESSAGE_COMPOSE(true, false);
	
	public final boolean canCompose;
	public final boolean canView;

	InteractionType() {
		this(true, true);
	}
	
	InteractionType(boolean canCompose, boolean canView) {
		this.canCompose = canCompose;
		this.canView = canView;
		
	}
}