package sneer.tuples;

import rx.functions.*;
import sneer.*;

public interface TuplePublisher extends Action1<Object>, Action0 {

	TuplePublisher audience(PublicKey audience);

	TuplePublisher type(String type);

	TuplePublisher value(Object value);
	
	TuplePublisher put(String key, Object value);
	
	/**
	 * Publishes an object. Equivalent to calling {@link #value(Object)} and {@link #pub()}.
	 * @param value
	 * @return the published tuple
	 */
	Tuple pub(Object value);

	Tuple pub();

}
