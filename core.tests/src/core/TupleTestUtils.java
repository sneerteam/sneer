package core;

import static core.ObservableTestUtils.*;
import static sneer.tuples.Tuple.*;
import rx.*;
import sneer.tuples.*;

public class TupleTestUtils {

	public static void expectValues(Observable<Tuple> tuples, Object... expecteds) {
		assertEqualsUntilNow(tuples.map(TO_PAYLOAD), expecteds);
	}

}
