package sneer;

import static sneer.ObservableTestUtils.*;
import static sneer.tuples.Tuple.*;
import rx.*;
import sneer.admin.*;
import sneer.impl.keys.*;
import sneer.refimpl.*;
import sneer.tuples.*;

public class TestsBase {
	
	private final Object session = createSession();

	protected final PrivateKey userA = Keys.createPrivateKey();
	protected final PrivateKey userB = Keys.createPrivateKey();
	protected final PrivateKey userC = Keys.createPrivateKey();
	
	protected final TupleSpace tuplesA = init(userA).tupleSpace();
	protected final TupleSpace tuplesB = init(userB).tupleSpace();
	protected final TupleSpace tuplesC = init(userC).tupleSpace();

	
	protected SneerAdmin createSneerAdmin(Object session) {
		return new SneerAdminInProcess((LocalTuplesFactory) session);
	}
	
	protected Object createSession() {
		return new TuplesFactoryInProcess();
	}
	
	private Sneer init(PrivateKey prik) {
		try {
			SneerAdmin admin = createSneerAdmin(session);
			admin.initialize(prik);
			return admin.sneer();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}


	protected void expectValues(Observable<Tuple> tuples, Object... expecteds) {
		assertEqualsUntilNow(tuples.map(TO_PAYLOAD), expecteds);
	}

}