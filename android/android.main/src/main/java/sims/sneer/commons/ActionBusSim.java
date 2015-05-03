package sims.sneer.commons;

import sneer.commons.ActionBus;

public class ActionBusSim implements ActionBus {

	@Override
	public void action(Object action) {
		System.out.println("Action: " + action);
	}

}