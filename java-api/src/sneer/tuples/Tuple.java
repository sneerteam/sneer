package sneer.tuples;

import java.util.*;

import rx.functions.*;
import sneer.*;

public interface Tuple extends Map<String, Object> {

	public static Func1<Tuple, Object> TO_VALUE = new Func1<Tuple, Object>() {  @Override public Object call(Tuple t) {
		return t.value();
	}};
	public static Func1<Tuple, String> TO_TYPE = new Func1<Tuple, String>() { @Override public String call(Tuple t) {
		return t.type();
	}};

	Object value();
	
	String type();

	PublicKey audience();

	PublicKey author();

}
