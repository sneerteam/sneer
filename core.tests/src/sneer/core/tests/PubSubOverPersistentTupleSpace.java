package sneer.core.tests;

import rx.functions.*;

public class PubSubOverPersistentTupleSpace extends PubSubTest {
	
	public static PubSubTest create(Func0<Object> tupleBaseFactory) {
		return new PubSubOverPersistentTupleSpace(tupleBaseFactory);
	}
	
	PubSubOverPersistentTupleSpace(Func0<Object> tupleBaseFactory) {
		super(tupleBaseFactory);
	}
	
	public PubSubOverPersistentTupleSpace() {
		this(new Func0<Object>() { @Override public Object call() {
			return Glue.var("sneer.core.tests.jdbc-tuple-base", "create").invoke();
		}});
	}

}
