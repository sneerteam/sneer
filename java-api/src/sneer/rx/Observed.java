package sneer.rx;

import rx.*;
import rx.functions.*;
import rx.subjects.*;

/** An Observable for which we know the most recent value. */
public class Observed<T> {
	
	private static final Object WAITING = new Object();
	private static final Func1<Object, Boolean> NOT_WAITING = new Func1<Object, Boolean>() { @Override public Boolean call(Object item) {
		return item != WAITING;
	}};
	
	private final BehaviorSubject<T> subject;
	
	
	@SuppressWarnings("unchecked")
	public Observed(Observable<T> observableThatWillEmitAValueImmediately) {
		this.subject = BehaviorSubject.create((T)WAITING);
		observableThatWillEmitAValueImmediately.subscribe(this.subject);
	}
	
	
	/** @return The most recent item emitted by observable(). */
	public T mostRecent() {
		return subject.filter(NOT_WAITING).toBlockingObservable().first();
	}
	
	
	/** @return The Observable being observed by this. */
	public Observable<T> observable() {
		return subject.asObservable();
	}	

}
