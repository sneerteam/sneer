package sneer.core.tests;

public class SimpleP2POverPersistentTupleSpace extends SimpleP2P {
	
	@Override
	protected Object newTupleBase() {
		return Glue.var("sneer.persistent-tuple-base", "create").invoke();
	}

}
