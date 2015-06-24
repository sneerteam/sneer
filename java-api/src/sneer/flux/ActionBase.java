package sneer.flux;

class ActionBase {

	public final String type;
	public final Object[] keyValuePairs;

	ActionBase(Object[] keyValuePairs, String type) {
		this.keyValuePairs = keyValuePairs;
		this.type = type;
	}
}
