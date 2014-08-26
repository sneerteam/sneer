package sneer.core.tests;

import sneer.android.main.core.*;

public class ConversationsAPIOverSqliteDatabase extends ConversationsAPITest {

	@Override
	protected Object newTupleBase() {
		return SneerSqliteDatabase.tmpTupleBase();
	}
}
