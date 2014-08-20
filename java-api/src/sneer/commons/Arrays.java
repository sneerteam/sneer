package sneer.commons;

import rx.functions.*;

public class Arrays {

	@SuppressWarnings("unchecked")
	public static <T, R> R[] map(T[] source, Func1<T, R> converter) {		
		Object[] r = new Object[source.length];
		for (int i = 0; i < r.length; i++)
			r[i] = converter.call(source[i]);
		return (R[]) r;
	}

}
