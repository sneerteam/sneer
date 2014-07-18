package sneer.impl.simulator;

import rx.*;
import sneer.*;
import sneer.impl.*;
import sneer.rx.*;

public class PartySimulator implements Party {

	private final ObservedSubject<PublicKey> publicKey;

	/** The name this Party gives itself. */
	private final ObservedSubject<String> name;

	
	
	public PartySimulator(PublicKey puk) {
		this.publicKey = ObservedSubject.create(puk);
		this.name = ObservedSubject.create("No name set yet (Puk " + puk + ")");
	}

	public PartySimulator(String partyName) {
		PrivateKey prik = Keys.newPrivateKey();
		this.publicKey = ObservedSubject.create(prik.publicKey());
		this.name = ObservedSubject.create(partyName);
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
