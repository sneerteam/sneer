package sneer.impl.simulator;

import static sneer.Contact.*;
import static sneer.Interaction.*;

import java.util.*;
import java.util.concurrent.*;

import rx.Observable;
import rx.functions.*;
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

	private TupleSpace tupleSpace;

	private PrivateKey privateKey;
	public static TuplesFactoryInProcess cloud = new TuplesFactoryInProcess();
	
	public SneerSimulator(PrivateKey privateKey) {
		this.privateKey = privateKey;
		self = new PartySimulator("Neide da Silva", privateKey);

		tupleSpace = cloud.newTupleSpace(privateKey);
		
		setupMockupRPSPlayer(cloud, addContact("Maicon Tesourinha"), "SCISSORS");
		setupMockupRPSPlayer(cloud, addContact("Wesley Pedreira"), "ROCK");
		setupMockupRPSPlayer(cloud, addContact("Carla Folhada"), "PAPER");
	}
	
	public PrivateKey privateKey() {
		return privateKey;
	}


	private void setupMockupRPSPlayer(TuplesFactoryInProcess cloud, PrivateKey playerPrik, final String move) {
		final TupleSpace tupleSpace = cloud.newTupleSpace(playerPrik);
		tupleSpace.filter().type("rock-paper-scissors/move").audience(playerPrik).tuples()
//			.delay(2, TimeUnit.SECONDS)
			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
				tupleSpace.publisher().type("rock-paper-scissors/move").audience(t1.author()).pub(move);
			}});
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
	public TupleSpace tupleSpace() {
		return tupleSpace;
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

	
	private PrivateKey addContact(String nick) {
		PrivateKey prik = Keys.createPrivateKey(nick);
		Party party = produceParty(prik.publicKey());
		((PartySimulator)party).setName(nick + " da Silva");
		setContact(nick, party);
		return prik;
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