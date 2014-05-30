package sneerteam.snapi;

import java.util.concurrent.*;

import rx.*;
import rx.functions.*;

public interface CloudPath {

	CloudPath append(Object segment);

	Observable<PathEvent> children();

	void pub();
	void pub(Object value);
	
	Observable<Object> value();
	
    void queryExistence(long timeout, TimeUnit unit, Action0 ifExist, Action0 ifAbsent);
    void ifAbsent(long timeout, TimeUnit unit, Action0 action);
	
}
