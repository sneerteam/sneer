package sneer.core.tests;

import static junit.framework.Assert.*;
import static sneer.tuples.Tuple.*;

import java.util.*;
import java.util.concurrent.*;

import rx.*;
import rx.Observable;
import rx.functions.*;
import rx.schedulers.*;
import sneer.tuples.*;

@SuppressWarnings("deprecation")
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
	
	public static Observable<Void> notifications(Observable<?> source, @SuppressWarnings("rawtypes") final Notification... values) {
		return values(source.materialize(), (Object[])values);
	}
	
	public static Observable<Void> eventually(Observable<?> source, final Object... values) {
		return values(
			source.skipWhile(new Func1<Object, Boolean>() { @Override public Boolean call(Object t1) {
				return !t1.equals(values[0]);
			}}),
			values);
	}

	public static Observable<Void> values(Observable<?> tuples, final Object... values) {
		return tuples
			.buffer(500, TimeUnit.MILLISECONDS, values.length)
			.map(new Func1<List<?>, Void>() { @Override public Void call(List<?> t1) {
				assertListSize(values, t1);
				assertArrayEquals(values, t1.toArray());
				return null;
			}});
	}
	
	public static Observable<Void> same(Observable<?> tuples, final Object... expecteds) {
		return tuples
			.buffer(500, TimeUnit.MILLISECONDS, expecteds.length)
			.map(new Func1<List<?>, Void>() { @Override public Void call(List<?> list) {
				assertListSize(expecteds, list);

				Iterator<?> it = list.iterator();
				for (Object expected : expecteds) {
					assertSame(expected, it.next());
				}
				return null;
			}});
	}
	
	@SafeVarargs
	public static <T> void assertEqualsUntilNow(Observable<T> seq, T... expecteds) {
		List<T> list = takeAllUntilNow(seq);
		assertList(expecteds, list);
	}
	
	public static <T> void assertList(T[] expected, List<T> list) {
		assertListSize(expected, list);
		Iterator<T> it = list.iterator();
		for (Object e : expected) {
			if (e.getClass().isArray()) {
				assertArrayEquals((Object[])e, (Object[])it.next());
			} else {
				assertEquals(e, it.next());
			}
		}
	}
	
	public static void assertArrayEquals(Object[] expected, Object[] actual) {
		assertList(expected, Arrays.asList(actual));
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

	private static void assertListSize(Object[] expecteds, List<?> actual) {
		if (expecteds.length != actual.size())
			fail("Expecting `" + Arrays.asList(expecteds) + "', got `" + actual + "'");
	}

	public static Func1<Tuple, Object> field(final String field) {
		return new Func1<Tuple, Object>() {  @Override public Object call(Tuple t1) {
			return t1.get(field);
		}};
	}

}
