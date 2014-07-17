package sneer.tuples;

import java.util.*;

import sneer.*;

public interface Tuple extends Map<String, Object> {

	Object value();
	
	String intent();

	PublicKey audience();

	PublicKey author();

}
