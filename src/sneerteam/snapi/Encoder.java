package sneerteam.snapi;

import static us.bpsm.edn.parser.Parsers.defaultConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import us.bpsm.edn.Keyword;
import us.bpsm.edn.parser.Parsers;
import us.bpsm.edn.printer.Printers;
import android.os.Bundle;

public class Encoder {
	
	public static final String VALUE = ":value";

	public static Bundle bundle(String edn) {
		return bundleValue(parse(edn));
	}	

	public static String unbundle(Bundle bundle) {
		return print(getValue(bundle));
	}

	static Object getValue(Bundle bundle) {
		return toEdnBuiltin(bundle.get(VALUE));
	}

	static String print(Object value) {
		return Printers.printString(Printers.defaultPrinterProtocol(), value);
	}

	static Object toEdnBuiltin(Object object) {
		if (object instanceof String) {
			String s = (String)object;
			return s.startsWith(":")
				? Keyword.newKeyword(s.substring(1))
				: s;
		}
		if (object instanceof Bundle) {
			return toMap((Bundle)object);
		}
		return object;
	}
	
	static Object toMap(Bundle bundle) {
		Set<String> keys = bundle.keySet();
		HashMap<Object, Object> map = new HashMap<Object, Object>(keys.size());
		for (String key : keys) {
			map.put(toEdnBuiltin(key), toEdnBuiltin(bundle.get(key)));
		}
		return map;
	}
	
	static Bundle bundleValue(Object edn) {
		Bundle bundle = new Bundle();
		putValue(bundle, VALUE, edn);
		return bundle;
	}

	static  void putValue(Bundle bundle, String key, Object value) {
		if (value instanceof String) {
			bundle.putString(key, (String) value);
		} else if (value instanceof Keyword) {
			bundle.putString(key, value.toString());
		} else if (value instanceof Long) {
			bundle.putLong(key, (Long)value);
		} else if (value instanceof Map) {
			bundle.putBundle(key, fromMap((Map<?, ?>)value));
		} else
			throw new UnsupportedOperationException();
	}
	
	static Bundle fromMap(Map<?, ?> map) {
		Bundle bundle = new Bundle();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			putValue(bundle, entry.getKey().toString(), entry.getValue());
		}
		return bundle;
	}
	
	static Object parse(String edn) {
		return Parsers
			.newParser(defaultConfiguration())
			.nextValue(Parsers.newParseable(edn));
	}
}
