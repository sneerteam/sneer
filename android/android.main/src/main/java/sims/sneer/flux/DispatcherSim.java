package sims.sneer.flux;

import java.util.Arrays;

import rx.Observable;
import sneer.flux.Action;
import sneer.flux.Dispatcher;
import sneer.flux.Request;

import static sims.sneer.convos.NotificationsSim.turnNotificationsOff;
import static sims.sneer.convos.NotificationsSim.turnNotificationsOn;

public class DispatcherSim implements Dispatcher {

	@Override
	public void dispatch(Action action) {
		System.out.println("Dispatching " + action.type + ": " + Arrays.toString(action.keyValuePairs));

		if (!action.type.equals("send-message")) return;
		if (Arrays.toString(action.keyValuePairs).contains("non"))  turnNotificationsOn();
		if (Arrays.toString(action.keyValuePairs).contains("noff")) turnNotificationsOff();
	}

	@Override
	public <T> Observable<T> request(Request<T> request) {
		throw new UnsupportedOperationException("Should not be used by the UI. This method will disappear when we have full-fledged flux.");
	}

}
