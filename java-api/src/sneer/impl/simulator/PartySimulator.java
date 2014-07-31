package sneer.impl.simulator;

import rx.*;
import rx.subjects.*;
import sneer.*;
import sneer.commons.exceptions.*;
import sneer.rx.*;


public class PartySimulator implements Party, Profile {

	private final ObservedSubject<PublicKey> publicKey;

	/** The name this Party gives itself. */
	private final BehaviorSubject<String> name;

	/** Profile */
	private Subject<String, String> preferredNickname = BehaviorSubject.create("neide");
	private Subject<String, String> city = BehaviorSubject.create("Jundiaí");
	private Subject<String, String> country = BehaviorSubject.create("Brasil");
	private ReplaySubject<byte[]> selfie = ReplaySubject.create();
	
	
	public PartySimulator(String name, PrivateKey prik) {
		this(name, prik.publicKey());
	}
	
	
	PartySimulator(PublicKey puk) {
		this("Neide da Silva", puk);
	}


	PartySimulator(String name, PublicKey puk) {
		this.publicKey = ObservedSubject.create(puk);
		this.name = BehaviorSubject.create(name);
	}


	@Override
	public Observed<PublicKey> publicKey() {
		return publicKey.observed();
	}

	
	@Override
	public Observable<String> name() {
		return name.asObservable();
	}

	
	public void setName(String newName) {
		name.onNext(newName);
	}

	public Observed<PrivateKey> privateKey() {
		return null;
	}

	
	/////////////////////// Profile
	@Override
	public Observable<String> preferredNickname() {	
		return preferredNickname;
	}


	@Override
	public void setPreferredNickname(String newPreferredNickname) {
		preferredNickname.onNext(newPreferredNickname);
	}


	@Override
	public Observable<byte[]> selfie() {
		return selfie;
	}


	@Override
	public void setSelfie(byte[] newSelfie) throws FriendlyException {
		selfie.onNext(newSelfie);
	}


	@Override
	public Observable<String> country() {
		return country;
	}


	@Override
	public void setCountry(String newCountry) {
		country.onNext(newCountry);
	}


	@Override
	public Observable<String> city() {
		return city;
	}


	@Override
	public void setCity(String newCity) {
		city.onNext(newCity);
	}

}
