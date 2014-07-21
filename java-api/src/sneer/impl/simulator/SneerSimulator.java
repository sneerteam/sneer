package sneer.impl.simulator;

import java.util.*;
import java.util.concurrent.*;

import rx.Observable;
import rx.subjects.*;
import sneer.*;
import sneer.impl.*;
import sneer.impl.keys.*;
import sneer.tuples.*;

public class SneerSimulator extends SneerBase {

	private final Map<PublicKey, Party> partiesByPuk = new ConcurrentHashMap<PublicKey, Party>();
	private final Map<Party, Interaction> interactionsByParty = new ConcurrentHashMap<Party, Interaction>();
	private final BehaviorSubject<List<Interaction>> interactions = BehaviorSubject.create(interactionsNow());
	private final PartySimulator self;

	
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
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setContact(String nickname, Party party) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Observable<List<Contact>> contacts() {
		// TODO Auto-generated method stub
		return null;
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

	
	private void populate(String... newContactNicks) {
		for (String nick : newContactNicks) {
			PrivateKey prik = Keys.createPrivateKey(nick);
			String name = nick + " da Silva";
			setContact(nick, new PartySimulator(name, prik));
		}
	}

}


