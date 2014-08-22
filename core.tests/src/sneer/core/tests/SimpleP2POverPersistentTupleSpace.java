package sneer.core.tests;

public class SimpleP2POverPersistentTupleSpace extends SimpleP2P {
	
	@Override
	protected Object newTupleBase() {
		return Glue.var("sneer.core.tests.jdbc-tuple-base", "create").invoke();
	}

}
