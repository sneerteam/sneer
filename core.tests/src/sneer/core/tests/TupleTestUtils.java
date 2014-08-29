package sneer.core.tests;

import static sneer.core.tests.ObservableTestUtils.*;
import static sneer.tuples.Tuple.*;
import rx.*;
import sneer.tuples.*;

public class TupleTestUtils {

	public static void expectValues(Observable<Tuple> tuples, Object... expecteds) {
		expecting(values(tuples.map(TO_PAYLOAD), expecteds));
	}

}
