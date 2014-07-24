package sneer.tuples;

import rx.*;
import rx.observables.*;
import sneer.*;

public interface TupleSubscriber {

	TupleSubscriber author(PublicKey author);

	TupleSubscriber audience(PrivateKey audience);
	
	TupleSubscriber type(String type);

	Observable<Tuple> tuples();
	Observable<Object> values();
	
	BlockingObservable<Tuple> localTuples();

	TupleSubscriber where(String key, Object value);

}
