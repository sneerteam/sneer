package sneer;

import static sneer.ObservableTestUtils.*;
import static sneer.commons.TupleUtils.*;
import rx.*;
import sneer.refimpl.*;

public class TestsBase {
	
	private final InProcessCloudFactory inProcessCloudFacory = new InProcessCloudFactory();
	
	Sneer sneerA = createSneer();
	Sneer sneerB = createSneer();
	Sneer sneerC = createSneer();
	
	KeyPair userA = sneerA.createKeyPair();
	KeyPair userB = sneerB.createKeyPair();
	KeyPair userC = sneerC.createKeyPair();
	
	Cloud cloudA = sneerA.newCloud(userA);
	Cloud cloudB = sneerB.newCloud(userB);
	Cloud cloudC = sneerC.newCloud(userC);

	
	protected Sneer createSneer() {
		return new Sneer() {
			@Override
			public KeyPair createKeyPair() {
				return inProcessCloudFacory.createKeyPair();
			}

			@Override
			public Cloud newCloud(KeyPair identity) {
				return inProcessCloudFacory.newCloud(identity);
			}
		};
	}

	
	void expectValues(Observable<Tuple> tuples, Object... expecteds) {
		assertEqualsUntilNow(tuples.map(TO_VALUE), expecteds);
	}

}