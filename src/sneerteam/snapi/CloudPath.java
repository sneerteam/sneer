package sneerteam.snapi;

import java.util.concurrent.*;

import rx.*;
import us.bpsm.edn.*;

public interface CloudPath {
	
	public static final Keyword ME = Keyword.newKeyword("me");
	
	CloudPath append(Object segment);

	Observable<PathEvent> children();

	void pub();
	void pub(Object value);
	
	Observable<Object> value();
	
	Observable<Boolean> exists(long timeout, TimeUnit unit);
	
}
