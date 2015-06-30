package sneer.flux;

public class ActionBase {

	public final String type;
	public final Object[] keyValuePairs;

	ActionBase(String type, Object[] keyValuePairs) {
		this.keyValuePairs = keyValuePairs;
		this.type = type;
	}
}
