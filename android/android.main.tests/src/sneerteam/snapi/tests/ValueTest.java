package sneerteam.snapi.tests;

import java.util.HashMap;
import java.util.Map;

import sneer.utils.*;
import us.bpsm.edn.*;
import android.os.Parcel;
import junit.framework.TestCase;

public class ValueTest extends TestCase {
	
	public void testString() {
		Value v = roundtrip(Value.of("42"));
		assertEquals("42", v.get());
	}
	
	public void testLong() {
		Value v = roundtrip(Value.of(42L));
		assertEquals(42L, v.get());
	}
	
	public void testMap() {
		Map<?, ?> map = hash_map("foo", "bar", "bar", "baz");
		Value v = roundtrip(Value.of(map));
		Map<?, ?> actual = (Map<?, ?>)v.get();
		assertMapEquals(map, actual);
	}
	
	public void testKeyword() {
		Keyword keyword = Keyword.newKeyword("me");
		Value v = roundtrip(Value.of(keyword));
		assertEquals(keyword, v.get());
	}

	private Map<?, ?> hash_map(Object... kvs) {
		HashMap<Object, Object> result = new HashMap<Object, Object>(kvs.length / 2);
		for (int i = 0; i < kvs.length; i += 2) {
			result.put(kvs[i], kvs[i + 1]);
		}
		return result;
	}

	private void assertMapEquals(Map<?, ?> map, Map<?, ?> actual) {
		assertEquals(map.keySet(), actual.keySet());
	}

	private Value roundtrip(Value v) {
		Parcel parcel = Parcel.obtain();
		try {
			parcel.writeParcelable(v, 0);
			parcel.setDataPosition(0);
			return (Value)parcel.readParcelable(getClass().getClassLoader());
		} finally {
			parcel.recycle();
		}
	}

}
