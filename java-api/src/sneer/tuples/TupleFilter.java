package sneer.tuples;

import java.util.*;

import rx.Observable;
import sneer.*;

public interface TupleFilter {

	TupleFilter author(PublicKey author);
	TupleFilter audience(PrivateKey audience);
	TupleFilter audienceMe();
	TupleFilter type(String type);
//	TupleFilter payload(Object payload);
	TupleFilter field(String field, Object value);
	TupleFilter putFields(Map<String, Object> fields);

	/** @return An observable that emits tuples satisfying this filter. The returned Observable does not complete. */
	Observable<Tuple> tuples();
	
	/** @return An observable that completes after the filtered tuples from the local device are emitted. */
	Observable<Tuple> localTuples();

}
