package sneer.impl.simulator;

import static sneer.Contact.*;
import static sneer.commons.exceptions.Exceptions.*;
import rx.*;
import rx.functions.*;
import rx.subjects.*;
import sneer.*;
import sneer.rx.*;


public class PartySimulator implements Party, Profile {

	private final ObservedSubject<PublicKey> publicKey;
	private final boolean isSelf;

	/** Profile */
	private final BehaviorSubject<String> ownName;
	private Subject<String, String> preferredNickname = BehaviorSubject.create("neide");
	private Subject<String, String> city = BehaviorSubject.create("Jundiaí");
	private Subject<String, String> country = BehaviorSubject.create("Brasil");
	private ReplaySubject<byte[]> selfie = ReplaySubject.create();
	private Sneer sneer;
	
	
	PartySimulator(String name, PublicKey puk) {
		this(name, puk, true);
	}


	PartySimulator(PublicKey puk, Sneer sneer) {
		this("? PublicKey: " + puk, puk, false);
		this.sneer = sneer;
	}


	PartySimulator(String name, PublicKey puk, boolean isSelf) {
		this.publicKey = ObservedSubject.create(puk);
		this.ownName = BehaviorSubject.create(name);
		this.isSelf = isSelf;
	}


	@Override
	public Observed<PublicKey> publicKey() {
		return publicKey.observed();
	}

	
	/////////////////////// Profile

	@Override
	public Observable<String> ownName() {
		return ownName.asObservable();
	}

	
	@Override
	public void setOwnName(String newName) {
		check(isSelf);
		simulateSetName(newName);
	}
	
	
	public void simulateSetName(String newName) {
		ownName.onNext(newName);
	}
	
	
	@Override
	public Observable<String> preferredNickname() {	
		return preferredNickname;
	}

	@Override
	public void setPreferredNickname(String newPreferredNickname) {
		check(isSelf);
		simulateSetPreferredNickname(newPreferredNickname);
	}


	protected void simulateSetPreferredNickname(String newPreferredNickname) {
		preferredNickname.onNext(newPreferredNickname);
	}


	@Override
	public Observable<byte[]> selfie() {
		return selfie;
	}


	@Override
	public void setSelfie(byte[] newSelfie) {
		check(isSelf);
		simulateSetSelfie(newSelfie);
	}


	protected void simulateSetSelfie(byte[] newSelfie) {
		selfie.onNext(newSelfie);
	}


	@Override
	public Observable<String> city() {
		return city;
	}


	@Override
	public void setCity(String newCity) {
		check(isSelf);
		simulateSetCity(newCity);
	}


	protected void simulateSetCity(String newCity) {
		city.onNext(newCity);
	}
	
	
	@Override
	public Observable<String> country() {
		return country;
	}
	
	
	@Override
	public void setCountry(String newCountry) {
		check(isSelf);
		simulateSetCountry(newCountry);
	}


	protected void simulateSetCountry(String newCountry) {
		country.onNext(newCountry);
	}


	@Override
	public Observable<String> name() {
		return ownName().concatWith(nickname());
	}
	

	private Observable<String> nickname() {
		return sneer.contacts().map(new Func1<Object, Contact>() { @Override public Contact call(Object ignored) {
			return findContact();
		}}).filter(new Func1<Contact, Boolean>() { @Override public Boolean call(Contact found) {
			return found != null;
		}}).flatMap(TO_NICKNAME);
	}
	

	private Contact findContact() {
		return sneer.findContact(this);
	}
}
