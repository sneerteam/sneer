package sneer.impl.simulator;

import static sneer.commons.exceptions.Exceptions.*;
import rx.*;
import rx.subjects.*;
import sneer.*;
import sneer.commons.exceptions.*;
import sneer.rx.*;


public class PartySimulator implements Party, Profile {

	private final ObservedSubject<PublicKey> publicKey;
	private final boolean isSelf;

	/** Profile */
	private final BehaviorSubject<String> name;
	private Subject<String, String> preferredNickname = BehaviorSubject.create("neide");
	private Subject<String, String> city = BehaviorSubject.create("Jundiaí");
	private Subject<String, String> country = BehaviorSubject.create("Brasil");
	private ReplaySubject<byte[]> selfie = ReplaySubject.create();

	
	
	public PartySimulator(String name, PrivateKey prik) {
		this(name, prik.publicKey(), true);
	}
	
	
	PartySimulator(PublicKey puk) {
		this("?", puk, false);
	}


	PartySimulator(String name, PublicKey puk, boolean isSelf) {
		this.publicKey = ObservedSubject.create(puk);
		this.name = BehaviorSubject.create(name);
		this.isSelf = isSelf;
	}


	@Override
	public Observed<PublicKey> publicKey() {
		return publicKey.observed();
	}

	
	/////////////////////// Profile

	@Override
	public Observable<String> name() {
		return name.asObservable();
	}
	
	
	@Override
	public void setName(String newName) {
		check(isSelf);
		name.onNext(newName);
	}
	
	@Override
	public Observable<String> preferredNickname() {	
		return preferredNickname;
	}


	@Override
	public void setPreferredNickname(String newPreferredNickname) {
		check(isSelf);
		preferredNickname.onNext(newPreferredNickname);
	}


	@Override
	public Observable<byte[]> selfie() {
		return selfie;
	}


	@Override
	public void setSelfie(byte[] newSelfie) throws FriendlyException {
		check(isSelf);
		selfie.onNext(newSelfie);
	}


	@Override
	public Observable<String> city() {
		return city;
	}


	@Override
	public void setCity(String newCity) {
		check(isSelf);
		city.onNext(newCity);
	}
	
	
	@Override
	public Observable<String> country() {
		return country;
	}
	
	
	@Override
	public void setCountry(String newCountry) {
		check(isSelf);
		country.onNext(newCountry);
	}

}
