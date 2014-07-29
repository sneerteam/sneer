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
	private BehaviorSubject<String> preferredNickname = BehaviorSubject.create("dude");
	private BehaviorSubject<String> city = BehaviorSubject.create("Campinas");
	private BehaviorSubject<String> country = BehaviorSubject.create("Brazil");
	private BehaviorSubject<byte[]> selfie;
	
	
	public PartySimulator(String name, PrivateKey prik) {
		this(name, prik.publicKey());
	}
	
	
	PartySimulator(PublicKey puk) {
		this("?", puk);
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
		return null;
	}


	@Override
	public void setSelfie(byte[] newSelfie) throws FriendlyException {
//		selfie.onNext(newSelfie);
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
