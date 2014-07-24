package sneer.tuples;

import rx.*;
import sneer.*;

public interface TupleSubscriber {

	TupleSubscriber author(PublicKey author);

	TupleSubscriber audience(PrivateKey audience);
	
	TupleSubscriber type(String type);

	Observable<Tuple> tuples();
	Observable<Object> values();
	
	/**
	 * @return an observable that completes after local tuples are emitted. 
	 */
	Observable<Tuple> localTuples();

	TupleSubscriber where(String key, Object value);

}
