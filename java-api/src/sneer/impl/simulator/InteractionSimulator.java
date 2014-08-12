package sneer.impl.simulator;

import static sneer.InteractionEvent.*;
import static sneer.InteractionMenuItem.*;
import static sneer.commons.Lists.*;

import java.util.*;

import rx.Observable;
import sneer.*;
import sneer.rx.*;

public class InteractionSimulator implements Interaction {

	private final Party party;
	
	@SuppressWarnings("unchecked")
	private final ObservedSubject<List<InteractionEvent>> events = ObservedSubject.create((List<InteractionEvent>)Collections.EMPTY_LIST);
	@SuppressWarnings("unchecked")
	private final ObservedSubject<List<InteractionMenuItem>> menuItems = ObservedSubject.create((List<InteractionMenuItem>)Collections.EMPTY_LIST);
	private final ObservedSubject<Long> mostRecentEventTimestamp = ObservedSubject.create(0L);
	private final ObservedSubject<String> mostRecentEventContent = ObservedSubject.create("");
	
	
	public InteractionSimulator(Party party) {
		this.party = party;
		sendMessage("Vai ter festa!!!! Uhuu!!!");
		simulateReceivedMessage("Onde? Onde?? o0");
		
		addMenuItem(new InteractionMenuItemSimulator("Send Bitcoins"));
		addMenuItem(new InteractionMenuItemSimulator("Play Toroidal Go"));
		addMenuItem(new InteractionMenuItemSimulator("Send my location"));
		addMenuItem(new InteractionMenuItemSimulator("Send photo"));
		addMenuItem(new InteractionMenuItemSimulator("Send voice message"));
		addMenuItem(new InteractionMenuItemSimulator("Play Rock Paper Scissors"));
		addMenuItem(new InteractionMenuItemSimulator("Play Tic Tac Toe"));
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
	
	
	@Override
	public Observed<String> mostRecentEventContent() {
		return mostRecentEventContent.observed();
	}
	
	
	private void addEvent(InteractionEvent event) {
		List<InteractionEvent> newEvents = eventsWith(event, BY_TIME_RECEIVED);
		events.set(newEvents);
		InteractionEvent last = lastIn(newEvents);
		mostRecentEventTimestamp.set(last.timestampReceived());
		mostRecentEventContent.set(last.content());
	}


	private List<InteractionEvent> eventsWith(InteractionEvent event, Comparator<InteractionEvent> order) {
		List<InteractionEvent> ret = new ArrayList<InteractionEvent>(events.observed().current());
		ret.add(event);
		Collections.sort(ret, order);
		return ret;
	}
	
	
	private void simulateReceivedMessage(String content) {
		addEvent(createFrom(now(), now(), content));
	}
	
	
	static private long now() {
		return System.currentTimeMillis();
	}


	@Override
	public Observed<List<InteractionMenuItem>> menu() {
		return menuItems.observed();
	}
	
	
	private void addMenuItem(InteractionMenuItem menuItem) {
		List<InteractionMenuItem> newMenuItems = menuItemsWith(menuItem, BY_ALPHABETICAL_ORDER);
		menuItems.set(newMenuItems);
	}
	
	
	private List<InteractionMenuItem> menuItemsWith(InteractionMenuItem menuItem, Comparator<InteractionMenuItem> order) {
		List<InteractionMenuItem> ret = new ArrayList<InteractionMenuItem>(menuItems.observed().current());
		ret.add(menuItem);
		Collections.sort(ret, order);
		return ret;
	}

}
