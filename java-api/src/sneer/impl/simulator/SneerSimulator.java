package sneer.impl.simulator;

import static sneer.Contact.*;

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
	private final BehaviorSubject<List<Interaction>> interactions = BehaviorSubject.create(interactionsNow());

	
	public SneerSimulator(PrivateKey privateKey) {
		self = new PartySimulator("Neide da Silva", privateKey);
		
		populate("Maicon", "Wesley", "Carla");
	}
	
	
	@Override
	public Party produceParty(PublicKey publicKey) {
		Party ret;
		synchronized (partiesByPuk) {
			ret = partiesByPuk.get(publicKey);
			if (ret == null) {
				//ret = new PartySimulator(publicKey);
				partiesByPuk.put(publicKey, ret);
			}
		}
		return ret;
	}


	@Override
	public Observable<List<Interaction>> interactions() {
		return interactions;
	}

	
	@Override
	public Interaction produceInteractionWith(Party party) {
		Interaction ret;
		synchronized (interactionsByParty) {
			ret = interactionsByParty.get(party);
			if (ret == null) {
				ret = new InteractionSimulator(party);
				interactionsByParty.put(party, ret);
				interactions.onNext(interactionsNow());
			}
		}
		return ret;
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
			}
			c.setNickname(nickname);
			notifyContactsSubscribers();
		}
	}


	private void notifyContactsSubscribers() {
		contacts.onNext(contactsSorted());
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
	
	
	private List<Interaction> interactionsNow() {
		return new ArrayList<Interaction>(interactionsByParty.values());
	}

	
	private List<Contact> contactsSorted() {
		ArrayList<Contact> ret = new ArrayList<Contact>(contactsByParty.values());
		Collections.sort(ret, BY_NICKNAME);
		return ret;
	}

	
	private void populate(String... newContactNicks) {
		for (String nick : newContactNicks) {
			PrivateKey prik = Keys.createPrivateKey(nick);
			String name = nick + " da Silva";
			setContact(nick, new PartySimulator(name, prik));
		}
	}

}