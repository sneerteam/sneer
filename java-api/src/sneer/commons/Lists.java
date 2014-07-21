package sneer.commons;

import java.util.*;

public class Lists {

	public static <T> T lastIn(List<T> list) {
		return list.get(list.size() - 1);
	}

}
