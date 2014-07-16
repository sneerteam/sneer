package sneer;

import rx.*;

public interface TupleSubscriber {

	TupleSubscriber author(PublicKey author);

	TupleSubscriber audience(PublicKey audience);
	
	TupleSubscriber intent(String intent);

	Observable<Tuple> tuples();
	Observable<Object> values();

	TupleSubscriber where(String key, Object value);

}
