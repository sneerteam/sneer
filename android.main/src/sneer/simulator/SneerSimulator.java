package sneer.simulator;

import static basis.rx.RXUtils.*;
import rx.*;
import rx.functions.*;
import rx.subjects.*;
import sneer.*;

public class SneerSimulator implements Sneer {

	private final Subject<Contact, Contact> contacts = ReplaySubject.create();
	private final PartySimulator self = new PartySimulator("Neide da Silva");

	{
		createExamples();
	}
	
	
	@Override
	public Self self() {
		return self;
	}


	@Override
	public Observable<Contact> contacts() {
		return contacts;
	}

	@Override
	public Contact findContact(final String publicKey) {
		return current(contacts.filter(new Func1<Contact, Boolean>() { @Override public Boolean call(Contact contact) {
			return current(contact.party().publicKey()).equals(publicKey);
		}}));
	}
	
	
	private void createExamples() {
		addContact("Neo", "Thomas Anderson");
		addContact("Morpheus", "Klaus Wuestefeld");
		addContact("Zero Dois", "Felipe Bueno");
		addContact("Zero Tres", "Diego Mendes");
		addContact("Xerife", "Fabio Roger");
	}
	

	private void addContact(String nickname, String partyName) {
		addContact(nickname, new PartySimulator(partyName));
	}

	
	@Override
	public void addContact(String nickname, Party party) {
		contacts.onNext(new ContactSimulator(nickname, party));
	}




	@Override
	public Observable<Party> parties() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public Party findParty(String publicKey) {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public Observable<Interaction> interactions() {
		// TODO Auto-generated method stub
		return null;
	}




	@Override
	public Interaction produceInteractionWith(Party party) {
		// TODO Auto-generated method stub
		return null;
	}

	
}
