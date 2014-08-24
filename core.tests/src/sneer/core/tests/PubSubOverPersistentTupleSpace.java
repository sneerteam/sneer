package sneer.core.tests;

import rx.functions.*;

public class PubSubOverPersistentTupleSpace extends PubSubTest {
	
	private final Func0<Object> tupleBaseFactory;

	PubSubOverPersistentTupleSpace(Func0<Object> tupleBaseFactory) {
		this.tupleBaseFactory = tupleBaseFactory;
	}
	
	public PubSubOverPersistentTupleSpace() {
		this(new Func0<Object>() {  @Override public Object call() {
			return Glue.var("sneer.core.tests.jdbc-tuple-base", "create").invoke();
		}});
	}
	
	@Override
	protected Object newTupleBase() {
		return tupleBaseFactory.call();
	}

}
