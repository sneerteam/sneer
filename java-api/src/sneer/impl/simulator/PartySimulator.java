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
		// TODO Auto-generated method stub
		
		// Usar BehaviorSubject, como o this.name usa.
		return null;
	}


	@Override
	public void setPreferredNickname(String newPreferredNickname) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Observable<byte[]> selfie() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setSelfie(byte[] newSelfie) throws FriendlyException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Observable<String> country() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setCountry(String newCountry) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Observable<String> city() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setCity(String newCity) {
		// TODO Auto-generated method stub
		
	}

}
