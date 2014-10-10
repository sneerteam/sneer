package sneer.tuples;

import java.util.Map;

import rx.functions.Func1;
import sneer.PublicKey;

public interface Tuple extends Map<String, Object> {

	public static Func1<Tuple, Object> TO_PAYLOAD = new Func1<Tuple, Object>() {  @Override public Object call(Tuple t) {
		return t.payload();
	}};
	public static Func1<Tuple, String> TO_TYPE = new Func1<Tuple, String>() { @Override public String call(Tuple t) {
		return t.type();
	}};

	Object payload();
	
	String type();

	PublicKey audience();

	PublicKey author();
	
	long timestampCreated();

}
