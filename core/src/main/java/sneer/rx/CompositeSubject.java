package sneer.rx;

import rx.Observer;
import rx.subjects.Subject;

public class CompositeSubject extends Subject {
	private final Observer observer;

	public CompositeSubject(OnSubscribe onSubscribe, Observer observer) {
		super(onSubscribe);
		this.observer = observer;
	}

	@Override
	public void onCompleted() {
		observer.onCompleted();
	}

	@Override
	public void onError(Throwable e) {
		observer.onError(e);
	}

	@Override
	public void onNext(Object o) {
		observer.onNext(o);
	}
}
