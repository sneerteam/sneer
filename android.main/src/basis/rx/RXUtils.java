package basis.rx;

import rx.*;

public class RXUtils {

	public static <T> T current(Observable<T> observable) {
		return observable.toBlockingObservable().first();
	}

}
