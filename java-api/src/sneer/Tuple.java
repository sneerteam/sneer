package sneer;

import java.util.*;

public interface Tuple extends Map<String, Object> {

	Object value();
	
	String intent();

	PublicKey audience();

	PublicKey author();

}
