package sneer.tuples;

import rx.functions.*;

public class TupleUtils {

	public static Func1<Tuple, Object> TO_VALUE = new Func1<Tuple, Object>() {  @Override public Object call(Tuple t) {
		return t.value();
	}};

	public static Func1<Tuple, String[]> TO_INTENT = new Func1<Tuple, String[]>() {  @Override public String[] call(Tuple t) {
		throw new RuntimeException("FixMe");
		//return t.intent();
	}};

}
