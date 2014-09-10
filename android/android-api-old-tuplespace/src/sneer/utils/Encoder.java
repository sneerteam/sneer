package sneer.utils;

import java.util.*;

import sneer.commons.exceptions.*;

public class Encoder {
	
	public static List<Object> pathDecode(Value[] path) {
		ArrayList<Object> result = new ArrayList<Object>(path.length);
		for (Value segment : path)
			result.add(segment.get());
		return result;
	}

	public static Value[] pathEncode(List<Object> segments) {
		Value[] result = new Value[segments.size()];
		int i = 0;
		for (Object s : segments)
			result[i++] = Value.of(s);
		return result;
	}
	
	static Object toKeywordOrString(String s) {
		if (s.startsWith(":"))
			throw new NotImplementedYet();
		else
			return s;
	}
}
