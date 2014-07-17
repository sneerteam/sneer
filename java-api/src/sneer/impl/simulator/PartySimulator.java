package sneer.impl.simulator;

import rx.*;
import rx.subjects.*;
import sneer.*;
import sneer.rx.*;

public class PartySimulator implements Self {

	private final Observed<PrivateKey> privateKey;
	private final Observed<PublicKey> publicKey;

	/** The name this Party gives itself. */
	private final BehaviorSubject<String> name;

	
	
	public PartySimulator(PrivateKey prik) {
		this.privateKey = new Observed<PrivateKey>(Observable.from(prik));
		this.publicKey = new Observed<PublicKey>(Observable.from(prik.publicKey()));
		this.name = BehaviorSubject.create("No name set yet (Puk " + prik.publicKey() + ")");
	}

	public PartySimulator(String partyName) {
		PrivateKey prik = Keys.newPrivateKey();
		this.privateKey = new Observed<PrivateKey>(Observable.from(prik));
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

	
	@Override
	public void setName(String newName) {
		name.onNext(newName);
	}

	@Override
	public Observed<PrivateKey> privateKey() {
		return null;
	}

}
