package sneer.core.tests;

import java.io.*;

import junit.framework.*;
import rx.functions.*;
import sneer.android.main.core.*;

public class PubSubOverSqliteAndroid extends TestCase {

	public void testMessagePassing() throws IOException {
		pubSubTest().messagePassing();
	}

	private PubSubTest pubSubTest() {
		return PubSubOverPersistentTupleSpace.create(new Func0<Object>() {  @Override public Object call() {
			try {
				return SneerSqliteDatabase.tmpTupleBase();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}});
	}
	
}
