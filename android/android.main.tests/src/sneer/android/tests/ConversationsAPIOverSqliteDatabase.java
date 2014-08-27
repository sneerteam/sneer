package sneer.android.tests;

import sneer.android.main.core.*;
import sneer.core.tests.*;

public class ConversationsAPIOverSqliteDatabase extends ConversationsAPITest {

	@Override
	protected Object newTupleBase() {
		return SneerTestUtils.tmpTupleBase();
	}
}
