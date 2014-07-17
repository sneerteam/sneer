package sneer;

import static sneer.ObservableTestUtils.*;
import static sneer.commons.TupleUtils.*;
import rx.*;
import sneer.refimpl.*;

public class TestsBase {
	
	private final InProcessCloudFactory factory = new InProcessCloudFactory();
	
	Sneer sneerA = createSneer();
	Sneer sneerB = createSneer();
	Sneer sneerC = createSneer();
	
	PrivateKey userA = sneerA.createPrivateKey();
	PrivateKey userB = sneerB.createPrivateKey();
	PrivateKey userC = sneerC.createPrivateKey();
	
	Cloud cloudA = sneerA.newCloud(userA);
	Cloud cloudB = sneerB.newCloud(userB);
	Cloud cloudC = sneerC.newCloud(userC);

	
	protected Sneer createSneer() {
		return new Sneer() {
			@Override
			public PrivateKey createPrivateKey() {
				return factory.createPrivateKey();
			}

			@Override
			public Cloud newCloud(PrivateKey identity) {
				return factory.newCloud(identity);
			}
		};
	}

	
	void expectValues(Observable<Tuple> tuples, Object... expecteds) {
		assertEqualsUntilNow(tuples.map(TO_VALUE), expecteds);
	}

}