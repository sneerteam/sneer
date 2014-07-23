package sneer.impl.simulator;

import static sneer.InteractionEvent.*;
import static sneer.commons.Lists.*;

import java.util.*;

import rx.Observable;
import sneer.*;
import sneer.rx.*;

public class InteractionSimulator implements Interaction {

	@SuppressWarnings("unchecked")
	private static final List<InteractionEvent> NO_EVENTS = Collections.EMPTY_LIST;

	
	private final Party party;
	private final ObservedSubject<List<InteractionEvent>> events = ObservedSubject.create(NO_EVENTS);
	private final ObservedSubject<Long> mostRecentEventTimestamp = ObservedSubject.create(0L);

	
	public InteractionSimulator(Party party) {
		this.party = party;
		sendMessage("Vai ter festa!!!! Uhuu!!!");
		simulateReceivedMessage("Onde? Onde??");
	}


	@Override
	public Party party() {
		return party;
	}

	
	@Override
	public Observable<List<InteractionEvent>> events() {
		return events.observed().observable();
	}

	
	@Override
	public void sendMessage(String content) {
		addEvent(createOwn(now(), content));
		simulateReceivedMessage("Echo: " + content);
	}


	@Override
	public Observed<Long> mostRecentEventTimestamp() {
		return mostRecentEventTimestamp.observed();
	}
	
	
	private void addEvent(InteractionEvent event) {
		List<InteractionEvent> newEvents = eventsWith(event, BY_TIME_RECEIVED);
		events.set(newEvents);
		mostRecentEventTimestamp.set(lastIn(newEvents).timestampReceived());
	}


	private List<InteractionEvent> eventsWith(InteractionEvent event, Comparator<InteractionEvent> order) {
		List<InteractionEvent> ret = new ArrayList<InteractionEvent>(events.observed().mostRecent());
		ret.add(event);
		Collections.sort(ret, order);
		return ret;
	}
	
	
	private void simulateReceivedMessage(String content) {
		addEvent(createFrom(this.party, now(), now(), content));
	}
	
	
	static private long now() {
		return System.currentTimeMillis();
	}

}
