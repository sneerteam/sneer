package sneer.android.tests;

import java.io.*;

import sneer.android.main.core.*;
import sneer.core.tests.*;

public class SneerAdminAndroidTest extends SneerAdminTest {
	
	@Override
	protected Object produceDatabase(File databaseFile) throws IOException {
		return SneerSqliteDatabase.openDatabase(databaseFile);
	}
	
}
