package sneer.android.utils;

import java.util.HashSet;
import java.util.Set;

public class EnumUtils {

	public static Set<String> names(Enum<?>[] values) {
		Set<String> names = new HashSet<String>();
		for (Enum<?> e : values)
			names.add(e.name());
		return names;
	}

}
