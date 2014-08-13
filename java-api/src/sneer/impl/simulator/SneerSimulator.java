package sneer.impl.simulator;

import static sneer.Contact.*;
import static sneer.Conversation.*;
import static sneer.commons.Streams.*;

import java.io.*;
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
	private final TupleSpace tupleSpace;
	private final PrivateKey privateKey;

	private final Map<PublicKey, PartySimulator> partiesByPuk = new ConcurrentHashMap<PublicKey, PartySimulator>();

	private final Map<Party, ContactSimulator> contactsByParty = new ConcurrentHashMap<Party, ContactSimulator>();
	private final BehaviorSubject<List<Contact>> contacts = BehaviorSubject.create(contactsSorted());

	private final Map<Party, Conversation> conversationsByParty = new ConcurrentHashMap<Party, Conversation>();
	private final BehaviorSubject<List<Conversation>> conversations = BehaviorSubject.create(conversationsSorted());

	
	public SneerSimulator(PrivateKey privateKey) {
		this.privateKey = privateKey;
		self = new PartySimulator("Neide da Silva", privateKey);
		self.setSelfie(selfieFromFileSystem("neide.png"));

		TuplesFactoryInProcess cloud = new TuplesFactoryInProcess();
		tupleSpace = cloud.newTupleSpace(privateKey);
		
		setupMockupRPSPlayer(cloud, addContact("Maicon Tesourinha", "maicon", "Paraguay", "Ciudad del Este", selfieFromFileSystem("maicon.jpg")), "SCISSORS");
		setupMockupRPSPlayer(cloud, addContact("Wesley Pedreira", "snypes", "USA", "Los Angeles", selfieFromFileSystem("wesley.jpg")), "ROCK");
		setupMockupRPSPlayer(cloud, addContact("Carla Folhada", "carlinha", "Brasil", "Florianopolis", selfieFromFileSystem("carla.jpg")), "PAPER");
	}


	public PrivateKey privateKey() {
		return privateKey;
	}


	private void setupMockupRPSPlayer(TuplesFactoryInProcess cloud, PrivateKey playerPrik, final String move) {
		final TupleSpace tupleSpace = cloud.newTupleSpace(playerPrik);
		tupleSpace.filter().type("rock-paper-scissors/move").audience(playerPrik).tuples()
			.delay(2, TimeUnit.SECONDS)
			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
				tupleSpace.publisher().type("rock-paper-scissors/move").audience(t1.author()).pub(move);
			}});
	}
	
	
	@Override
	public PartySimulator produceParty(PublicKey puk) {
		synchronized (partiesByPuk) {
			PartySimulator existing = partiesByPuk.get(puk);
			if (existing != null) return existing;
			
			PartySimulator newParty = new PartySimulator(puk);
			partiesByPuk.put(puk, newParty);
			return newParty;
		}
	}


	@Override
	public Observable<List<Conversation>> conversations() {
		return conversations;
	}

	
	@Override
	public Conversation produceConversationWith(Party party) {
		synchronized (conversationsByParty) {
			Conversation existing = conversationsByParty.get(party);
			if (existing != null) return existing;
			
			ConversationSimulator newConversation = new ConversationSimulator(party);
			conversationsByParty.put(party, newConversation);
			conversations.onNext(conversationsSorted());
			return newConversation;
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
	public void addContact(String nickname, Party party) {
		synchronized (contactsByParty) {
			if (contactsByParty.get(party) != null)
				throw new RuntimeException("The party you tried to add was already a contact.");

			ContactSimulator c = new ContactSimulator(nickname, party);
			contactsByParty.put(party, c);
			produceConversationWith(party).sendMessage("Hey " + nickname + "!");
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
		self.setOwnName(newName);
	}
	
	
	private List<Conversation> conversationsSorted() {
		ArrayList<Conversation> ret = new ArrayList<Conversation>(conversationsByParty.values());
		Collections.sort(ret, MOST_RECENT_FIRST);
		return ret;
	}

	
	private List<Contact> contactsSorted() {
		ArrayList<Contact> ret = new ArrayList<Contact>(contactsByParty.values());
		Collections.sort(ret, BY_NICKNAME);
		return ret;
	}

	
	private PrivateKey addContact(String name, String preferredNicknane, String coutry, String city, byte[] selfie) {
		PrivateKey prik = Keys.createPrivateKey(name);
		PartySimulator party = produceParty(prik.publicKey());
		party.simulateSetName(name);
		party.simulateSetPreferredNickname(preferredNicknane);
		party.simulateSetCountry(coutry);
		party.simulateSetCity(city);
		party.simulateSetSelfie(selfie);
		addContact(name, party);
		return prik;
	}
	

	@Override
	public Observable<List<Conversation>> conversationsContaining(String messageType) {
		return conversations();
	}


	@Override
	public Profile profileFor(Party party) {
		return (PartySimulator)party;
	}
	
	
	private byte[] selfieFromFileSystem(String fileName) {
		try {
			return readFully(getClass().getResourceAsStream(fileName));
		} catch (IOException e) {
//			TODO - não fazer nada quando der erro
			throw new RuntimeException(e);
		}
	}


	@Override
	public Contact findContact(String nickname) {
		throw new UnsupportedOperationException("Not needed until now.");
	}


	@Override
	public WritableContact writable(Contact contact) {
		return (WritableContact)contact;
	}

}