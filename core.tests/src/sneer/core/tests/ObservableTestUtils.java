package sneer.core.tests;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.fail;
import static sneer.tuples.Tuple.TO_PAYLOAD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Notification;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.schedulers.TestScheduler;
import sneer.tuples.Tuple;

@SuppressWarnings("deprecation")
public class ObservableTestUtils {

	public static void expecting(Observable<?>... expectations) {
		Observable
			.merge(expectations)
			.buffer(expectations.length)
			.timeout(expectations.length * 2, TimeUnit.SECONDS)
			.toBlocking()
			.first();
	}

	public static Observable<Void> payloads(Observable<Tuple> tuples, final Object... values) {
		return values(tuples.map(TO_PAYLOAD), values);
	}

	public static Observable<Void> payloadSet(Observable<Tuple> tuples, final Object... values) {
		return valueSet(tuples.map(TO_PAYLOAD), values);
	}

	public static Observable<Void> notifications(Observable<?> source, @SuppressWarnings("rawtypes") final Notification... values) {
		return values(source.materialize(), (Object[])values);
	}

	public static Observable<Void> eventually(Observable<?> source, final Object... expected) {
		return values(
			source.skipWhile(new Func1<Object, Boolean>() { @Override public Boolean call(Object obj) {
				return !obj.equals(expected[0]);
			}}),
			expected);
	}

	public static Observable<Void> values(Observable<?> source, final Object... expected) {
		return values(source, SEQUENCE, expected);
	}

	public static Observable<Void> valueSet(Observable<?> source, final Object... expected) {
		return values(source, SET, expected);
	}

	public static Observable<Void> same(Observable<?> source, final Object... expected) {
		return values(source, SAME_ELEMENTS, expected);
	}

	private static Observable<Void> values(Observable<?> source, final Action2<Object[], List<?>> assertion, final Object... expected) {
		return source
			.buffer(2, TimeUnit.SECONDS, expected.length)
			.map(new Func1<List<?>, Void>() { @Override public Void call(List<?> actual) {
				assertion.call(expected, actual);
				return null;
			}});
	}

	static final Action2<Object[], List<?>> SEQUENCE = new Action2<Object[], List<?>>() {  @Override public void call(Object[] expected, List<?> actual) {
		assertList(expected, actual);
	}};

	static final Action2<Object[], List<?>> SET = new Action2<Object[], List<?>>() {  @Override public void call(Object[] expected, List<?> actual) {
		assertSet(expected, actual);
	}};

	static final Action2<Object[], List<?>> SAME_ELEMENTS = new Action2<Object[], List<?>>() {  @Override public void call(Object[] expected, List<?> actual) {
		assertSameElements(expected, actual);
	}};

	public static void assertSameElements(Object[] expected, List<?> list) {
		assertListSize(expected, list);
		Iterator<?> it = list.iterator();
		for (Object o : expected) {
			assertSame(o, it.next());
		}
	}

	public static <T> void assertList(T[] expected, List<?> list) {
		assertListSize(expected, list);
		Iterator<?> it = list.iterator();
		for (Object e : expected) {
			if (e.getClass().isArray()) {
				assertArrayEquals((Object[])e, (Object[])it.next());
			} else {
				assertEquals(expectingMessage(expected, list), e, it.next());
			}
		}
	}

	public static void assertSet(Object[] expected, List<?> actual) {
		assertSet(Arrays.asList(expected), actual);
	}

	public static void assertSet(List<Object> expected, List<?> actual) {
		if (!expected.containsAll(actual) || !actual.containsAll(expected))
			fail(expectingMessage(actual, expected));
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
		}});
		scheduler.triggerActions();
		return result;
	}

	private static void assertListSize(Object[] expecteds, List<?> actual) {
		if (expecteds.length != actual.size())
			fail(expectingMessage(expecteds, actual));
	}

	private static String expectingMessage(Object[] expected, List<?> actual) {
		return expectingMessage(Arrays.asList(expected), actual);
	}

	private static String expectingMessage(List<?> expected, List<?> actual) {
		return "Expecting `" + expected + "', got `" + actual + "'";
	}

	public static Func1<Tuple, Object> field(final String field) {
		return new Func1<Tuple, Object>() {  @Override public Object call(Tuple tuple) {
			return tuple.get(field);
		}};
	}

}
