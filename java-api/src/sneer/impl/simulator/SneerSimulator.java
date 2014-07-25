package sneer.impl.simulator;

import static sneer.Contact.*;
import static sneer.Interaction.*;

import java.util.*;
import java.util.concurrent.*;

import rx.Observable;
import rx.subjects.*;
import sneer.*;
import sneer.impl.*;
import sneer.impl.keys.*;
import sneer.tuples.*;

public class SneerSimulator extends SneerBase {

	private final PartySimulator self;

	private final Map<PublicKey, Party> partiesByPuk = new ConcurrentHashMap<PublicKey, Party>();

	private final Map<Party, ContactSimulator> contactsByParty = new ConcurrentHashMap<Party, ContactSimulator>();
	private final BehaviorSubject<List<Contact>> contacts = BehaviorSubject.create(contactsSorted());

	private final Map<Party, Interaction> interactionsByParty = new ConcurrentHashMap<Party, Interaction>();
	private final BehaviorSubject<List<Interaction>> interactions = BehaviorSubject.create(interactionsSorted());

	
	public SneerSimulator(PrivateKey privateKey) {
		self = new PartySimulator("Neide da Silva", privateKey);

		populate("Maicon", "Wesley", "Carla");
	}
	
	
	@Override
	public Party produceParty(PublicKey puk) {
		synchronized (partiesByPuk) {
			Party existing = partiesByPuk.get(puk);
			if (existing != null) return existing;
			
			Party newParty = new PartySimulator(puk);
			partiesByPuk.put(puk, newParty);
			return newParty;
		}
	}


	@Override
	public Observable<List<Interaction>> interactions() {
		return interactions;
	}

	
	@Override
	public Interaction produceInteractionWith(Party party) {
		synchronized (interactionsByParty) {
			Interaction existing = interactionsByParty.get(party);
			if (existing != null) return existing;
			
			InteractionSimulator newInteraction = new InteractionSimulator(party);
			interactionsByParty.put(party, newInteraction);
			interactions.onNext(interactionsSorted());
			return newInteraction;
		}
	}
	

	@Override
	public Party self() {
		return self;
	}


	@Override
	public Contact findContact(Party party) {
		return contactsByParty.get(party);
	}


	@Override
	public void setContact(String nickname, Party party) {
		synchronized (contactsByParty) {
			ContactSimulator c = contactsByParty.get(party);
			if (c == null) {
				c = new ContactSimulator(nickname, party);
				contactsByParty.put(party, c);
				produceInteractionWith(party).sendMessage("Hey " + nickname + "!");
			}
			c.setNickname(nickname);
			contacts.onNext(contactsSorted());
		}
	}


	@Override
	public Observable<List<Contact>> contacts() {
		return contacts.asObservable();
	}


	@Override
	public Tuples tuples() {
		// TODO Auto-generated method stub
		return null;
	}


	public void setOwnName(String newName) {
		self.setName(newName);
	}
	
	
	private List<Interaction> interactionsSorted() {
		ArrayList<Interaction> ret = new ArrayList<Interaction>(interactionsByParty.values());
		Collections.sort(ret, MOST_RECENT_FIRST);
		return ret;
	}

	
	private List<Contact> contactsSorted() {
		ArrayList<Contact> ret = new ArrayList<Contact>(contactsByParty.values());
		Collections.sort(ret, BY_NICKNAME);
		return ret;
	}

	
	private void populate(String... newContactNicks) {
		for (String nick : newContactNicks) {
			PrivateKey prik = Keys.createPrivateKey(nick);
			Party party = produceParty(prik.publicKey());
			((PartySimulator)party).setName(nick + " da Silva");
			setContact(nick, party);
		}
	}


	@Override
	public Observable<List<Interaction>> interactionsContaining(String eventType) {
		return interactions();
	}


	@Override
	public Profile profileFor(Party party) {
		return (PartySimulator)party;
	}

}