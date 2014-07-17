package sneer.refimpl;

import java.io.*;
import java.util.*;

import rx.Observable;
import sneer.*;
import sneer.admin.*;
import sneer.commons.*;
import sneer.rx.*;
import sneer.tuples.*;

public class SneerAdminInProcess implements SneerAdmin {

	private final TuplesFactoryInProcess factory;
	private PrivateKey privateKey;

	
	public SneerAdminInProcess(TuplesFactoryInProcess factory) {
		this.factory = factory;
	}

	@Override
	public Sneer initialize(PrivateKey prik) throws WrongPrivateKey, IOException {
		Exceptions.check(privateKey == null);
		privateKey = prik;
		
		return new Sneer() {

			private final Tuples tuples = factory.newTuples(privateKey);

			@Override
			public Tuples tuples() {
				return tuples;
			}
			
			@Override public Party self() { return untested(); }
			@Override public Party produceParty(PublicKey publicKey) { return untested(); }
			@Override public Interaction produceInteractionWith(Party party) { return untested(); }
			@Override public Observed<String> labelFor(Party party) { return untested(); }
			@Override public Observable<Collection<Interaction>> interactions() { return untested(); }
			@Override public Contact findContact(Party party) { return untested(); }
			@Override public Observable<Collection<Contact>> contacts() { return untested(); }
			@Override public void addContact(String nickname, Party party) { untested(); }
		};
	}

	@Override
	public PrivateKey privateKey() {
		return privateKey;
	}

	@Override
	public void setOwnName(String newName) {
		untested();
	}
	
	static private <T> T untested() {
		throw new RuntimeException("This is not being tested yet.");
	}

}

