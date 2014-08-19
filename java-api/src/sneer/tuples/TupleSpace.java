package sneer.tuples;

import rx.*;

public interface TupleSpace {

	TuplePublisher publisher();

	TupleFilter filter();

//	Observable<Void> lease(Observable<Void> computation);

}
