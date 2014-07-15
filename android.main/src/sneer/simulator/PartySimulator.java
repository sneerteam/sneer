package sneer.simulator;

import rx.*;
import rx.subjects.*;
import sneer.*;

public class PartySimulator implements Self {

	static private int nextPublicKey = 100;

	private final Subject<String, String> publicKey;
	private final Subject<String, String> name;
	
	public PartySimulator(String name) {
		this.publicKey = BehaviorSubject.create("" + nextPublicKey++);
		this.name = BehaviorSubject.create(name);
	}

	
	@Override
	public Observable<String> publicKey() {
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
