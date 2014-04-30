package sneerteam.snapi;

import rx.*;

public interface EventualPath {

	EventualPath append(Object segment);

	Observable<PathEvent> children();

	void pub();
	void pub(Object value);
	
	Observable<Object> value();

}
