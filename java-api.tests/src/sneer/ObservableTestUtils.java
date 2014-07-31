package sneer;

import static org.junit.Assert.*;

import java.util.*;

import rx.Observable;
import rx.functions.*;
import rx.schedulers.*;

public class ObservableTestUtils {
	
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
