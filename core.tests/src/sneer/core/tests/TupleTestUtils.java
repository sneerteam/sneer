package sneer.core.tests;

import rx.Observable;
import sneer.tuples.Tuple;

import static sneer.core.tests.ObservableTestUtils.expecting;
import static sneer.core.tests.ObservableTestUtils.values;
import static sneer.tuples.Tuple.TO_PAYLOAD;

public class TupleTestUtils {

	public static void expectValues(Observable<Tuple> tuples, Object... expecteds) {
		expecting(values(tuples.map(TO_PAYLOAD), expecteds));
	}

}
