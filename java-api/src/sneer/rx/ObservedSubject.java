package sneer.rx;

import rx.*;
import rx.functions.*;
import rx.subjects.*;


public class ObservedSubject<T> {

	public static <T> ObservedSubject<T> create(T initialValue) {
		return new ObservedSubject<T>(initialValue);
	}

	
	volatile
	private T mostRecent;
	
	private final BehaviorSubject<T> subject;
	

	private ObservedSubject(T initialValue) {
		mostRecent = initialValue;
		subject = BehaviorSubject.create(initialValue);
		subject.subscribe(new Action1<T>() { @Override public void call(T newValue) {
			mostRecent = newValue;
		}});
	}

	
	public Observed<T> observed() {
		return new Observed<T>() {

			@Override
			public T mostRecent() {
				return mostRecent;
			}

			@Override
			public Observable<T> observable() {
				return subject.asObservable();
			}
		};
	}

	
	public void set(T newValue) {
		subject.onNext(newValue);
	}


	public Subject<T, T> subject() {
		return subject;
	}
	
}