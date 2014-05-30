package sneerteam.snapi;

import java.util.concurrent.*;

import rx.*;
import rx.functions.*;
import us.bpsm.edn.*;

public interface CloudPath {
	
	public static final Keyword ME = Keyword.newKeyword("me");
	
	CloudPath append(Object segment);

	Observable<PathEvent> children();

	void pub();
	void pub(Object value);
	
	Observable<Object> value();
	
    void queryExistence(long timeout, TimeUnit unit, Action0 ifExist, Action0 ifAbsent);
    void ifAbsent(long timeout, TimeUnit unit, Action0 action);
	
}
