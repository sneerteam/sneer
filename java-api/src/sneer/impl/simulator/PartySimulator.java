package sneer.impl.simulator;

import rx.*;
import sneer.*;
import sneer.rx.*;

public class PartySimulator implements Party {

	private final ObservedSubject<PublicKey> publicKey;

	/** The name this Party gives itself. */
	private final ObservedSubject<String> name;
	
	
	public PartySimulator(String name, PrivateKey prik) {
		this.publicKey = ObservedSubject.create(prik.publicKey());
		this.name = ObservedSubject.create(name);
	}
	
	
	@Override
	public Observed<PublicKey> publicKey() {
		return publicKey.observed();
	}

	
	@Override
	public Observable<String> name() {
		return name.observed().observable();
	}

	
	public void setName(String newName) {
		name.set(newName);
	}

	public Observed<PrivateKey> privateKey() {
		return null;
	}

}
