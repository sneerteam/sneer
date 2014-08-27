package sneer.core.tests;

import java.io.*;

import sneer.android.main.core.*;

public class SneerAdminAndroidTest extends SneerAdminTest {
	
	@Override
	protected Object produceDatabase(File databaseFile) throws IOException {
		return SneerSqliteDatabase.openDatabase(databaseFile);
	}
	
}
