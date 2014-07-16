package sneer.simulator;

import rx.*;
import rx.subjects.*;
import sneer.*;
import sneer.cloudnew.*;
import sneer.keys.PublicKey;
import sneer.rx.*;

public class PartySimulator implements Self {

	private final Observed<PublicKey> publicKey;

	/** The name this Party gives itself. */
	private final BehaviorSubject<String> name;
	
	
	public PartySimulator(PrivateKey prik) {
		this.publicKey = new Observed<PublicKey>(Observable.from(prik.publicKey()));
		this.name = BehaviorSubject.create("No name set yet (Puk " + puk + ")");
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

}
