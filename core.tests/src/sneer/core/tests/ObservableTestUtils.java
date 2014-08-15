package sneer.core.tests;

import static org.junit.Assert.*;
import static sneer.tuples.Tuple.*;

import java.util.*;
import java.util.concurrent.*;

import rx.Observable;
import rx.functions.*;
import rx.schedulers.*;
import sneer.tuples.*;

public class ObservableTestUtils {

	public static void expecting(Observable<?>... expectations) {
		Observable
			.merge(expectations)
			.buffer(expectations.length)
			.timeout(1, TimeUnit.SECONDS)
			.toBlocking()
			.first();
	}
	
	public static Observable<Void> payloads(Observable<Tuple> tuples, final Object... values) {
		return values(tuples.map(TO_PAYLOAD), values);
	}

	public static Observable<Void> values(Observable<?> tuples, final Object... values) {
		return tuples
			.buffer(500, TimeUnit.MILLISECONDS, values.length)
			.map(new Func1<List<?>, Void>() { @Override public Void call(List<?> t1) {
				assertArrayEquals(values, t1.toArray());
				return null;
			}});
	}
	
	@SafeVarargs
	public static <T> void assertEqualsUntilNow(Observable<T> seq, T... expecteds) {
		List<T> list = takeAllUntilNow(seq);
		if (expecteds.length != list.size())
			fail("Expecting `" + Arrays.asList(expecteds) + "', got `" + list + "'");
		Iterator<T> it = list.iterator();
		for (Object expected : expecteds) {
			if (expected.getClass().isArray()) {
				assertArrayEquals((Object[])expected, (Object[])it.next());
			} else {
				assertEquals(expected, it.next());
			}
		}
	}
	
	public static <T> void assertCount(int expected, Observable<T> seq) {
		assertEquals(expected, takeAllUntilNow(seq).size());
	}
	
	public static <T> List<T> takeAllUntilNow(Observable<T> seq) {
		TestScheduler scheduler = new TestScheduler();
		final List<T> result = new ArrayList<T>();
		seq.subscribeOn(scheduler).subscribe(new Action1<T>() {  @Override public void call(T item) {
			result.add(item);
		} });
		scheduler.triggerActions();
		return result;
	}

}
