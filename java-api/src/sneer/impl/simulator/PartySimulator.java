package sneer.impl.simulator;

import rx.*;
import rx.subjects.*;
import sneer.*;
import sneer.impl.*;
import sneer.rx.*;

public class PartySimulator implements Party {

	private final Observed<PublicKey> publicKey;

	/** The name this Party gives itself. */
	private final BehaviorSubject<String> name;

	
	
	public PartySimulator(PublicKey puk) {
		this.publicKey = new Observed<PublicKey>(Observable.from(puk));
		this.name = BehaviorSubject.create("No name set yet (Puk " + puk + ")");
	}

	public PartySimulator(String partyName) {
		PrivateKey prik = Keys.newPrivateKey();
		this.publicKey = new Observed<PublicKey>(Observable.from(prik.publicKey()));
		this.name = BehaviorSubject.create(partyName);
	}
	
	
	@Override
	public Observed<PublicKey> publicKey() {
		return publicKey;
	}

	
	@Override
	public Observable<String> name() {
		return name;
	}

	
	public void setName(String newName) {
		name.onNext(newName);
	}

	public Observed<PrivateKey> privateKey() {
		return null;
	}

}
