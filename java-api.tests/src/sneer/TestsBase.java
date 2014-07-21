package sneer;

import static sneer.ObservableTestUtils.*;
import rx.*;
import sneer.admin.*;
import sneer.impl.*;
import sneer.impl.keys.*;
import sneer.refimpl.*;
import sneer.tuples.*;

public class TestsBase {
	
	private final Object session = createSession();

	protected final PrivateKey userA = Keys.createPrivateKey();
	protected final PrivateKey userB = Keys.createPrivateKey();
	protected final PrivateKey userC = Keys.createPrivateKey();
	
	protected final Tuples tuplesA = init(userA).tuples();
	protected final Tuples tuplesB = init(userB).tuples();
	protected final Tuples tuplesC = init(userC).tuples();

	
	protected SneerAdmin createSneerAdmin(Object session) {
		return new SneerAdminInProcess((TuplesFactoryInProcess) session);
	}
	
	protected Object createSession() {
		return new TuplesFactoryInProcess();
	}
	
	private Sneer init(PrivateKey prik) {
		try {
			return createSneerAdmin(session).initialize(prik);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}


	protected void expectValues(Observable<Tuple> tuples, Object... expecteds) {
		assertEqualsUntilNow(tuples.map(TupleUtils.TO_VALUE), expecteds);
	}

}