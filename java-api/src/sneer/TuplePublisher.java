package sneer;

import rx.functions.*;

public interface TuplePublisher extends Action1<Object>, Action0 {

	TuplePublisher audience(PublicKey audience);

	TuplePublisher intent(String intent);

	TuplePublisher value(Object value);
	
	TuplePublisher put(String key, Object value);
	
	/**
	 * Publishes an object. Equivalent to calling {@link #value(Object)} and {@link #pub()}.
	 * @param value
	 * @return this
	 */
	TuplePublisher pub(Object value);

	TuplePublisher pub();

}
