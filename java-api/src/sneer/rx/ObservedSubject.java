package sneer.rx;

import rx.*;
import rx.functions.*;
import rx.subjects.*;


public class ObservedSubject<T> implements Observer<T> {

	/** It is expected that the subject will emit a value before the first call to current. */
	public static <T> ObservedSubject<T> createWithSubject(Subject<T, T> subject) {
		return new ObservedSubject<T>(subject);
	}

	public static <T> ObservedSubject<T> create(T initialValue) {
		return new ObservedSubject<T>(initialValue);
	}

	
	volatile
	private T mostRecent;
	
	private final Subject<T, T> subject;
	

	private ObservedSubject(T initialValue) {
		this(BehaviorSubject.create(initialValue));
	}
	
	public ObservedSubject(Subject<T, T> subject) {
		this.subject = subject;
		subject.subscribe(new Action1<T>() { @Override public void call(T newValue) {
			mostRecent = newValue;
		}});
	}

	public Observed<T> observed() {
		return new Observed<T>() {

			@Override
			public T current() {
				return mostRecent;
			}

			@Override
			public Observable<T> observable() {
				return subject.asObservable();
			}
		};
	}

	
	public Observable<T> observable() {
		return subject.asObservable();
	}

	
	public T current() {
		return mostRecent;
	}


	@Override
	public void onCompleted() {
		subject.onCompleted();
	}

	
	@Override
	public void onError(Throwable e) {
		subject.onError(e);
	}

	
	@Override
	public void onNext(T newValue) {
		subject.onNext(newValue);
	}
	
}