package sneer.commons;

import java.util.*;

import rx.functions.*;

public class Arrays {
	
	public static <T> List<T> asList(T... values) {
		return java.util.Arrays.asList(values);
	}

	public static <T, R> Object[] map(T[] source, Func1<T, R> converter) {
		Object[] r = new Object[source.length];
		for (int i = 0; i < r.length; i++)
			r[i] = converter.call(source[i]);
		return r;
	}

}
