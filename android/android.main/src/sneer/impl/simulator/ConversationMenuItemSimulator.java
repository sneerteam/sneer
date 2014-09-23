package sneer.impl.simulator;

import sneer.ConversationMenuItem;
import sneer.PublicKey;

public class ConversationMenuItemSimulator implements ConversationMenuItem {
	
	private String caption;
	private byte[] icon;
	
	ConversationMenuItemSimulator(String caption) {
		this.caption = caption;
	}


	@Override
	public void call(PublicKey partyPuk) {
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
