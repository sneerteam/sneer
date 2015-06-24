package sneer.flux;

public class Action extends ActionBase {

	Action(String type, Object[] keyValuePairs) {
		super(keyValuePairs, type);
	}

	public static Action action(String type, Object... keyValuePairs) {
		return new Action(type, keyValuePairs);
	}
}
