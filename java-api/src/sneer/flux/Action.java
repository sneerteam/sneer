package sneer.flux;

public class Action {

	public final String type;
	public final Object[] keyValuePairs;

	private Action(String type, Object[] keyValuePairs) {
		this.type = type;
		this.keyValuePairs = keyValuePairs;
	}

	public static Action action(String type, Object... keyValuePairs) {
		return new Action(type, keyValuePairs);
	}
}
