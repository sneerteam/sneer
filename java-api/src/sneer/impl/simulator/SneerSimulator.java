package sneer.impl.simulator;

import java.util.*;
import java.util.concurrent.*;

import rx.Observable;
import rx.subjects.*;
import sneer.*;
import sneer.impl.*;
import sneer.tuples.*;

public class SneerSimulator extends SneerBase {

	private final Map<PublicKey, Party> partiesByPuk = new ConcurrentHashMap<PublicKey, Party>();
	private final Map<Party, Interaction> interactionsByParty = new ConcurrentHashMap<Party, Interaction>();
	private final BehaviorSubject<Collection<Interaction>> interactions = BehaviorSubject.create(interactionsNow());
	private final PartySimulator self;

	
	public SneerSimulator(PublicKey puk) {
		self = new PartySimulator(puk);
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
	public Observable<Collection<Interaction>> interactions() {
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
	public void addContact(String nickname, Party party) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Observable<Collection<Contact>> contacts() {
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
	
	
	private Collection<Interaction> interactionsNow() {
		return new HashSet<Interaction>(interactionsByParty.values());
	}

	
}
