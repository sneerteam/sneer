package sneer.simulator;

import rx.*;
import sneer.*;

public class IndividualSimulator implements Individual {

	private Observable<String> publicKey;
	private Observable<String> name;

	public IndividualSimulator(Observable<String> publicKey, Observable<String> name) {
		this.publicKey = publicKey;
		this.name = name;
	}

	@Override
	public Observable<String> publicKey() {
		return publicKey;
	}


	@Override
	public Observable<String> name() {
		return name;
	}

}
