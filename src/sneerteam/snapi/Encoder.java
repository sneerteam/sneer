package sneerteam.snapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import us.bpsm.edn.Keyword;
import android.os.Bundle;

public class Encoder {
	
	public static List<Object> pathDecode(Bundle[] path) {
		ArrayList<Object> result = new ArrayList<Object>(path.length);
		for (Bundle segment : path)
			result.add(get(segment, "segment"));
		return result;
	}

	public static Bundle[] pathEncode(List<Object> segments) {
		Bundle[] result = new Bundle[segments.size()];
		int i = 0;
		for (Object s : segments)
			result[i++] = segment(s);
		return result;
	}

	public static Bundle segment(Object o) {
		return bundle("segment", o);
	}
	
	public static Bundle value(Object o) {
		return bundle(":value", o);
	}
	
	public static Object unbundle(Bundle b) {
		return get(b, ":value");
	}
	
	public static Object get(Bundle bundle, String key) {
		return toValue(bundle.get(key));
	}
	
	static Object toValue(Object object) {
		if (object instanceof String)
			return toKeywordOrString((String)object);
		if (object instanceof Bundle)
			return toMap((Bundle)object);
		return object;
	}
	
	static Object fromValue(Object value) {
		if (value instanceof Keyword)
			return value.toString();
		if (value instanceof Map)
			return fromMap((Map<?, ?>)value);
		if (value instanceof List)
			return fromList((List<?>)value);
		return value;
	}

	static Object toKeywordOrString(String s) {
		return s.startsWith(":")
			? Keyword.newKeyword(s.substring(1))
			: s;
	}
	
	public static Map<?, ?> toMap(Bundle bundle) {
		Set<String> keys = bundle.keySet();
		HashMap<Object, Object> map = new HashMap<Object, Object>(keys.size());
		for (String key : keys)
			map.put(toValue(key), toValue(bundle.get(key)));
		return map;
	}
	
	static Object fromList(List<?> values) {
		ArrayList<Object> result = new ArrayList<Object>(values.size());
		for (Object v : values)
			result.add(fromValue(v));
		return result;
	}

	public static Bundle fromMap(Map<?, ?> map) {
		Bundle bundle = new Bundle();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object value = entry.getValue();
			if (value == null) continue;
			putInBundleFromValue(bundle, entry.getKey().toString(), value);
		}
		return bundle;
	}

	static void putInBundleFromValue(Bundle bundle, String key, Object value) {
		if (value instanceof String) {
			bundle.putString(key, (String) value);
			return;
		}
		
		if (value instanceof Keyword) {
			bundle.putString(key, value.toString());
			return;
		}
		
		if (value instanceof Map) {
			bundle.putBundle(key,  fromMap((Map<?, ?>)value));
			return;
		}
		
		if (value instanceof Long) {
			bundle.putLong(key, (Long) value);
			return;
		}
		
		if (value instanceof Integer) {
			bundle.putInt(key, (Integer) value);
			return;
		}
		
		if (value instanceof Boolean) {
			bundle.putBoolean(key, (Boolean) value);
			return;
		}
		
		throw new UnsupportedOperationException("Cannot decode value `" + value + "' of type `" + (value == null ? "(unknown)" : value.getClass()) + "'.");
	}
	
	private static Bundle bundle(String key, Object value) {
		Bundle bundle = new Bundle();
		putInBundleFromValue(bundle, key, value);
		return bundle;
	}
}
