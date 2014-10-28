package sneer.impl.simulator;

import static sneer.Conversation.MOST_RECENT_FIRST;
import static sneer.commons.Streams.readFully;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Action1;
import rx.subjects.BehaviorSubject;
import sneer.Contact;
import sneer.Conversation;
import sneer.ConversationMenuItem;
import sneer.Party;
import sneer.PrivateKey;
import sneer.Profile;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.TuplesFactoryInProcess;
import sneer.tuples.Tuple;
import sneer.tuples.TupleSpace;

public class SneerSimulator implements Sneer {

	private final PartySimulator self;
	private final TupleSpace tupleSpace;

	private final Map<PublicKey, PartySimulator> partiesByPuk = new ConcurrentHashMap<PublicKey, PartySimulator>();

	private final Map<Party, ContactSimulator> contactsByParty = new ConcurrentHashMap<Party, ContactSimulator>();
	private final BehaviorSubject<List<Contact>> contacts = BehaviorSubject.create(contactsSorted());
	private final Map<Party, ContactSimulator> contactsSneer = new ConcurrentHashMap<Party, ContactSimulator>();
	
	private final Map<Party, Conversation> conversationsByParty = new ConcurrentHashMap<Party, Conversation>();
	private final BehaviorSubject<List<Conversation>> conversations = BehaviorSubject.create(conversationsSorted());
	
	private static final Comparator<Contact> BY_NICKNAME = new Comparator<Contact>() { @Override public int compare(Contact c1, Contact c2) {
		return c1.nickname().current().compareToIgnoreCase(c2.nickname().current());
	}};
	private PrivateKey prik;
	
	public SneerSimulator(PrivateKey prik) {
		
		this.prik = prik;
		self = new PartySimulator(prik.publicKey());
		self.setSelfie(selfieFromFileSystem("selfie_001.png"));

		TuplesFactoryInProcess cloud = new TuplesFactoryInProcess();
		tupleSpace = cloud.newTupleSpace(prik);
		
		setupMockupRPSPlayer(cloud, addContact("Tesourinha", "maicon", "Paraguay", "Ciudad del Este", selfieFromFileSystem("selfie_002.png")), "SCISSORS");
		setupMockupRPSPlayer(cloud, addContact("Pedreira", "snypes", "USA", "Los Angeles", selfieFromFileSystem("selfie_002.png")), "ROCK");
		setupMockupRPSPlayer(cloud, addContact("Folhada", "carlinha", "Brasil", "Florianopolis", selfieFromFileSystem("selfie_002.png")), "PAPER");
		
		addUnknownContact("Ze Ninguem", "dude", "World", "Unknown", selfieFromFileSystem("selfie_002.png"));
	}


	private void setupMockupRPSPlayer(TuplesFactoryInProcess cloud, PrivateKey playerPrik, final String move) {
		final TupleSpace tupleSpace = cloud.newTupleSpace(playerPrik);
		
		tupleSpace.filter().type("rock-paper-scissors/move").audience(playerPrik).tuples()
			.delay(1, TimeUnit.SECONDS)
			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
				tupleSpace.publisher()
					.type("rock-paper-scissors/move")
					.audience(t1.author())
					.field("session", t1.get("session"))
					.pub(move);
			}});
	}
	
	
	@Override
	public PartySimulator produceParty(PublicKey puk) {
		synchronized (partiesByPuk) {
			PartySimulator existing = partiesByPuk.get(puk);
			if (existing != null) return existing;
			
			final PartySimulator newParty = new PartySimulator(puk, this);
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
	public void addContact(final String nickname, final Party party) {		
		synchronized (contactsByParty) {
			if (contactsByParty.get(party) != null)
				throw new RuntimeException("The party you tried to add was already a contact.");
			
			ContactSimulator c = new ContactSimulator(nickname, party);
			contactsByParty.put(party, c);
			produceConversationWith(party).sendText("Hey " + nickname + "!");
			contacts.onNext(contactsSorted());
		}
		
		tupleSpace.publisher()
			.audience(prik.publicKey())
			.type("contact")
			.field("party", party.publicKey().current())
			.pub(nickname);
	}
	

	@Override
	public Observable<List<Contact>> contacts() {
		return contacts.asObservable();
	}
	

	static private int counter;
	@Override
	public String problemWithNewNickname(String newNick) {
		return (counter++ % 3 == 0) ? "Not cool" : null;
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
		PrivateKey prik = KeysSimulator.createPrivateKey();
		PartySimulator party = produceParty(prik.publicKey());
		addContact(name, party);
		return prik;
	}
	
	private PrivateKey addUnknownContact(String name, String preferredNicknane, String coutry, String city, byte[] selfie) {
		PrivateKey prik = KeysSimulator.createPrivateKey();
		PartySimulator party = produceParty(prik.publicKey());
		System.out.println("==============================");
		System.out.println("http://sneer.me/public-key?" + party.publicKey().current().toHex());
		System.out.println("==============================");
		addUnknownContact(name, party);
		return prik;
	}
	
	public void addUnknownContact(final String nickname, final Party party) {		
		synchronized (contactsSneer) {
			if (contactsSneer.get(party) != null)
				throw new RuntimeException("The party you tried to add was already a contact.");
			
			ContactSimulator c = new ContactSimulator(nickname, party);
			contactsSneer.put(party, c);
		}
		
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
		byte[] ret = null;
		try {
			ret = readFully(getClass().getResourceAsStream(fileName));
		} catch (IOException e) {}
		return ret;
	}


	@Override
	public void setConversationMenuItems(List<ConversationMenuItem> menuItems) {
		ConversationSimulator.menu.onNext(menuItems);
	}

}