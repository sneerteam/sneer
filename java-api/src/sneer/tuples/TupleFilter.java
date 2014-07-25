package sneer.tuples;

import rx.*;
import sneer.*;

public interface TupleFilter {

	TupleFilter byAuthor(PublicKey author);
	TupleFilter byAudience(PrivateKey audience);
	TupleFilter byType(String type);
	TupleFilter byField(String field, Object value);

	/** @return An observable that emits tuples satisfying this filter. The returned Observable does not complete. */
	Observable<Tuple> tuples();
	
	/** @return An observable that completes after the filtered tuples from the local device are emitted. */
	Observable<Tuple> localTuples();

}
