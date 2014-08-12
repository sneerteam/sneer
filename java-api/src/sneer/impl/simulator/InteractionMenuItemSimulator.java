package sneer.impl.simulator;

import sneer.*;

public class InteractionMenuItemSimulator implements InteractionMenuItem {
	
	private String caption;
	private byte[] icon;
	
	InteractionMenuItemSimulator(String caption) {
		this.caption = caption;
	}


	@Override
	public void call() {
		System.out.println(caption + " called");
	}

	
	@Override
	public String caption() {
		return caption;
	}

	
	@Override
	public byte[] icon() {
		return icon;
	}

}
