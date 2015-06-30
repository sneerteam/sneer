package sneer.flux;

public class Request<T> extends ActionBase {

	Request(String type, Object[] keyValuePairs) {
		super(type, keyValuePairs);
	}

	public static <T> Request<T> request(String type, Object... keyValuePairs) {
		return new Request<T>(type, keyValuePairs);
	}
}
