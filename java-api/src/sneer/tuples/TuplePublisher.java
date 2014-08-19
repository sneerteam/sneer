package sneer.tuples;

import java.util.*;

import rx.Observable;
import rx.functions.*;
import sneer.*;
import sneer.commons.*;

public interface TuplePublisher extends Action1<Object>, Action0 {

	TuplePublisher audience(PublicKey audience);
	TuplePublisher type(String type);
	TuplePublisher payload(Object payload);
	TuplePublisher field(String field, Object value);
	TuplePublisher putFields(Map<String, Object> fields);
	
	/** Publishes a tuple with the given payload. Equivalent to calling {@link #payload(Object)} and {@link #pub()}.
	 * @return The published tuple. */
	Tuple pub(Object payload);

	/** Publishes a tuple with the values set in this publisher.
	 * @return The published tuple. */
	Tuple pub();

//	/**
//	 * Atomic publication of local tuples.
//	 * 
//	 * @param condition a sequence that must emit True if payload should be published, and False or empty otherwise. Condition errors are propagated and will cancel the publication.
//	 * @param newPayload payload to be set if condition emits True
//	 * @return sequence that emits pair {true, newPayload} or {false, currentPayload}. Internal or condition errors will be propagated.
//	 */
//	Observable<Pair<Boolean, Object>> pubIf(Observable<Boolean> condition, Object newPayload);
//
//	Observable<Pair<Boolean, Object>> pubIfEmpty(TupleFilter condition, Object newPayload);

}
