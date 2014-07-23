package sneer.impl.simulator;

import rx.*;
import rx.subjects.*;
import sneer.*;
import sneer.rx.*;

public class PartySimulator implements Party {

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

}
