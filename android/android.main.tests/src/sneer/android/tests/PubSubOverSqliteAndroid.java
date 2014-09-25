package sneer.android.tests;

import java.io.IOException;

import junit.framework.TestCase;
import rx.functions.Func0;
import sneer.core.tests.PubSubOverPersistentTupleSpace;
import sneer.core.tests.PubSubTest;

public class PubSubOverSqliteAndroid extends TestCase {

	public void testMessagePassing() throws IOException {
		pubSubTest().messagePassing();
	}

	public void testPayloadTypeRepresentation() throws IOException {
		pubSubTest().payloadTypeRepresentation();
	}

	public void testCustomFieldTypeRepresentation() throws IOException {
		pubSubTest().customFieldTypeRepresentation();
	}
	
	private PubSubTest pubSubTest() {
		return PubSubOverPersistentTupleSpace.create(new Func0<Object>() {  @Override public Object call() {
			return TupleBaseFactory.tempTupleBase();
		}});
	}
	
}
