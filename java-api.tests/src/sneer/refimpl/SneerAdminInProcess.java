package sneer.refimpl;

import static sneer.commons.exceptions.Exceptions.*;

import java.util.*;

import rx.Observable;
import sneer.*;
import sneer.admin.*;
import sneer.rx.*;
import sneer.tuples.*;

public class SneerAdminInProcess implements SneerAdmin {

	private final TuplesFactoryInProcess factory;
	private PrivateKey privateKey;
	private Sneer sneer;

	
	public SneerAdminInProcess(TuplesFactoryInProcess factory) {
		this.factory = factory;
	}

	@Override
	public void initialize(PrivateKey prik) {
		check(privateKey == null);
		privateKey = prik;

		sneer = new Sneer() {

			private final TupleSpace tuples = factory.newTuples(privateKey);

			@Override
			public TupleSpace tupleSpace() {
				return tuples;
			}
			
			@Override public Party self() { return untested(); }
			@Override public Party produceParty(PublicKey publicKey) { return untested(); }
			@Override public Interaction produceInteractionWith(Party party) { return untested(); }
			@Override public Observed<String> labelFor(Party party) { return untested(); }
			@Override public Observable<List<Interaction>> interactions() { return untested(); }
			@Override public Contact findContact(Party party) { return untested(); }
			@Override public Observable<List<Contact>> contacts() { return untested(); }
			@Override public void setContact(String nickname, Party party) { untested(); }
			@Override public Observable<List<Interaction>> interactionsContaining(String eventType) { return untested(); }
			@Override public Profile profileFor(Party party) { return untested();	}
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

	@Override
	public Sneer sneer() {
		check(sneer != null);
		return sneer;
	}

}

