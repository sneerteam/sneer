package sneer.core.tests;

public class PubSubOverPersistentTupleSpace extends PubSubTest {
	
	@Override
	protected Object newTupleBase() {
		return Glue.var("sneer.core.tests.jdbc-tuple-base", "create").invoke();
	}

}
