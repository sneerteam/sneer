package sneer.tuples;

import rx.Observable;
import sneer.PrivateKey;
import sneer.PublicKey;

import java.util.Map;

public interface TupleFilter {

	TupleFilter author(PublicKey author);
	TupleFilter audience(PrivateKey audience);
	TupleFilter audience(PublicKey audience);
	TupleFilter type(String type);
	TupleFilter field(String field, Object value);
	TupleFilter putFields(Map<String, Object> fields);

	/** Causes the filter to emit tuples starting from the last known one to satisfy this filter. */
	TupleFilter last();

	/** @return An observable that emits tuples satisfying this filter. The returned Observable does not complete. */
	Observable<Tuple> tuples();
	
	/** @return An observable that completes after the filtered tuples from the local device are emitted. */
	Observable<Tuple> localTuples();

}
