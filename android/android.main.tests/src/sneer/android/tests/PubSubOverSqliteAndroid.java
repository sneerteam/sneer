package sneer.android.tests;

import java.io.*;

import junit.framework.*;
import rx.functions.*;
import sneer.core.tests.*;

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
