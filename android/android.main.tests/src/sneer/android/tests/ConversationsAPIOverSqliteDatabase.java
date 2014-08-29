package sneer.android.tests;

import sneer.core.tests.*;

public class ConversationsAPIOverSqliteDatabase extends ConversationsAPITest {

	@Override
	protected Object newTupleBase() {
		return TupleBaseFactory.tempTupleBase();
	}
}
