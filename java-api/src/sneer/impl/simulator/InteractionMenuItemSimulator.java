package sneer.impl.simulator;

import sneer.*;

public class InteractionMenuItemSimulator implements InteractionMenuItem {
	
	private String caption;
	private byte[] icon;
	
	@Override
	public void call() {
		// TODO Auto-generated method stub
	}

	
	@Override
	public String caption() {
		return caption;
	}

	
	@Override
	public byte[] icon() {
		return icon;
	}


	public void simulateSetCaption(String caption) {
		this.caption = caption;
	}

	public void simulateSetIcon(byte[] icon) {
		this.icon = icon;
	}
	
}
