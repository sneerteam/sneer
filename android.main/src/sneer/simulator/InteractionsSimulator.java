package sneer.simulator;

import java.util.*;

import rx.Observable;
import rx.functions.*;
import rx.subjects.*;
import sneer.*;

public class InteractionsSimulator implements Sneer {
	
	private final ReplaySubject<Party> parties = ReplaySubject.create();
	private final ReplaySubject<Interaction> interactions = ReplaySubject.create();
	private final Map<Party, Interaction> interactionsByParty = new HashMap<Party, Interaction>();

	
	@Override
	public Observable<Party> parties() {
		return parties;
	}

	
	@Override
	public Party findParty(final String publicKey) {
		return first(parties.filter(new Func1<Party, Boolean>() { @Override public Boolean call(Party party) {
			return first(party.publicKey()).equals(publicKey);
		}}));
		
		//return findIndividualParty();
	}


	@Override
	public Observable<Interaction> interactions() {
		return interactions;
	}

	
	@Override
	public Interaction produceInteractionWith(Party party) {
		Interaction ret = interactionsByParty.get(party);
		if (ret == null) {
			ret = new InteractionSimulator(party);
			interactionsByParty.put(party, ret);
			interactions.onNext(ret);
		}
		return ret;
	}
	
	
	static private <T> T first(Observable<T> observable) {
		return observable.toBlockingObservable().first();
	}

	private Party findIndividualParty(){
		//public IndividualSimulator(Observable<String> publicKey, Observable<String> nickname,Observable<String> name) {
		return new IndividualSimulator(Observable.from("P00001"), Observable.from("Segunda-feira"));
	}


	@Override
	public Self self() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Observable<Contact> contacts() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Contact findContact(String publicKey) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void addContact(String nickname, Party party) {
		// TODO Auto-generated method stub
		
	}
	
}
