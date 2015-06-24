package sims.sneer.commons;

import java.util.Arrays;

import sneer.flux.Action;
import sneer.flux.Dispatcher;

public class DispatcherSim implements Dispatcher {

	@Override
	public void dispatch(Action action) {
		System.out.println("Dispatching " + action.type + ": " + Arrays.toString(action.keyValuePairs));
	}

}