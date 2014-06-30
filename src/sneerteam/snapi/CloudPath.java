package sneerteam.snapi;

import java.util.*;
import java.util.concurrent.*;

import rx.Observable;
import us.bpsm.edn.*;

public interface CloudPath {
	
	public static final Keyword ME = Keyword.newKeyword("me");
	
	CloudPath append(Object segment);	
	CloudPath appends(Object... segments);	
	CloudPath appends(List<Object> segments);

	Observable<PathEvent> children();

	void pub();
	void pub(Object value);
	
	Observable<Object> value();
	
	Observable<Boolean> exists(long timeout, TimeUnit unit);
	
}
