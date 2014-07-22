package sneer.tuples;

import java.util.*;

import sneer.*;

public interface Tuple extends Map<String, Object> {

	Object value();
	
	String type();

	PublicKey audience();

	PublicKey author();

}
