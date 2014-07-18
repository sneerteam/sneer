package sneer.tuples;

import rx.*;
import sneer.*;

public interface TupleSubscriber {

	TupleSubscriber author(PublicKey author);

	TupleSubscriber audience(PrivateKey audience);
	
	TupleSubscriber intent(String intent);

	Observable<Tuple> tuples();
	Observable<Object> values();

	TupleSubscriber where(String key, Object value);

}
