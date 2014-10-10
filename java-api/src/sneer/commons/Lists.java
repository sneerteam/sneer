package sneer.commons;

import java.util.List;

public class Lists {

	public static String join(List<?> list, String separator) {
		StringBuilder ret = new StringBuilder();
		for (Object element : list) {
			if (ret.length() > 0) ret.append(separator);
			ret.append(element);
		}
		return ret.toString();
	}
	
	public static <T> T lastIn(List<T> list) {
		return list.isEmpty() ? null : list.get(list.size() - 1);
	}

}
