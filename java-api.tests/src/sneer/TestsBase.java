package sneer;

import static sneer.ObservableTestUtils.*;
import static sneer.commons.TupleUtils.*;
import rx.*;

public class TestsBase {
	
	Sneer sneer = SneerFactory.newSneer();

	KeyPair userA = sneer.newKeyPair();
	KeyPair userB = sneer.newKeyPair();
	KeyPair userC = sneer.newKeyPair();
	
	Cloud cloudA = sneer.newCloud(userA);
	Cloud cloudB = sneer.newCloud(userB);
	Cloud cloudC = sneer.newCloud(userC);

	void expectValues(Observable<Tuple> tuples, Object... expecteds) {
		assertEqualsUntilNow(tuples.map(TO_VALUE), expecteds);
	}

}