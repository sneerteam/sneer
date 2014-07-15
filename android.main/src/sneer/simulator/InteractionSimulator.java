package sneer.simulator;

import rx.*;
import rx.subjects.*;
import sneer.*;

public class InteractionSimulator implements Interaction {

	private final ReplaySubject<InteractionEvent> interactionEvents = ReplaySubject.create();

	private Party party;

	
	public InteractionSimulator(Party party) {
		this.party = party;
		sendInteractionEvent("q festa!!!! uhuu!!!");
		interactionEvents.onNext(new InteractionEvent(now(), now(), this.party, "Onde? Onde??"));
	}

	
	@Override
	public Party contact() {
		return party;
	}

	
	@Override
	public Observable<InteractionEvent> interactionEvents() {
		return interactionEvents;
	}

	
	@Override
	public void sendInteractionEvent(String content) {
		interactionEvents.onNext(new InteractionEvent(now(), 0, party, content));
	}
	
	
	static private long now() {
		return System.currentTimeMillis();
	}


	@Override
	public long lastInteractionEventTimestamp() {
		return 0;
	}


	@Override
	public Party contact(Party party) {
		// TODO Auto-generated method stub
		return null;
	}

}
