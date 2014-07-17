package sneer.impl.simulator;

import rx.*;
import rx.subjects.*;
import sneer.*;

public class InteractionSimulator implements Interaction {

	private final ReplaySubject<InteractionEvent> interactionEvents = ReplaySubject.create();

	private Party party;

	
	public InteractionSimulator(Party party) {
		this.party = party;
		sendMessage("q festa!!!! uhuu!!!");
		interactionEvents.onNext(new InteractionEvent(now(), now(), this.party, "Onde? Onde??"));
	}

	
	@Override
	public Party party() {
		return party;
	}

	
	@Override
	public Observable<InteractionEvent> events() {
		return interactionEvents;
	}

	
	@Override
	public void sendMessage(String content) {
		interactionEvents.onNext(new InteractionEvent(now(), 0, party, content));
	}
	
	
	static private long now() {
		return System.currentTimeMillis();
	}


	@Override
	public long mostRecentEventTimestamp() {
		return 0;
	}

}
