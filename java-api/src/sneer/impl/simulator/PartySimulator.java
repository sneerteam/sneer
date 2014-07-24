package sneer.impl.simulator;

import rx.*;
import rx.subjects.*;
import sneer.*;
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


	@Override
	public String firstName() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String lastName() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String country() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String city() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setFirstName(String firstName) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setLastName(String lastName) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setCountry(String country) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void setCity(String city) {
		// TODO Auto-generated method stub
		
	}

}
