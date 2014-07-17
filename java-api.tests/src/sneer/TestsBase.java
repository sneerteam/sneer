package sneer;

import static sneer.ObservableTestUtils.*;
import rx.*;
import sneer.admin.*;
import sneer.impl.*;
import sneer.refimpl.*;
import sneer.tuples.*;

public class TestsBase {
	
	private final TuplesFactoryInProcess factory = new TuplesFactoryInProcess();
	
	protected final PrivateKey userA = Keys.newPrivateKey();
	protected final PrivateKey userB = Keys.newPrivateKey();
	protected final PrivateKey userC = Keys.newPrivateKey();
	
	protected final Tuples tuplesA = init(userA).tuples();
	protected final Tuples tuplesB = init(userB).tuples();
	protected final Tuples tuplesC = init(userC).tuples();

	
	protected SneerAdmin createSneerAdmin() {
		return new SneerAdminInProcess(factory);
	}
	
	
	private Sneer init(PrivateKey prik) {
		try {
			return createSneerAdmin().initialize(prik);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}


	protected void expectValues(Observable<Tuple> tuples, Object... expecteds) {
		assertEqualsUntilNow(tuples.map(TupleUtils.TO_VALUE), expecteds);
	}

}