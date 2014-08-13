package sneer.impl.simulator;

import sneer.*;

public class ConversationMenuItemSimulator implements ConversationMenuItem {
	
	private String caption;
	private byte[] icon;
	
	ConversationMenuItemSimulator(String caption) {
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
