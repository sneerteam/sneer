package sneer.core.tests;

import static sneer.ClojureUtils.*;
import static sneer.core.tests.Glue.*;

import java.io.*;

import junit.framework.*;
import sneer.*;
import sneer.admin.*;

public class SneerAdminTest extends TestCase {
	
	public void testRememberKeys() throws IOException {
		
		File databaseFile = File.createTempFile("sneer", "db");
		
		Object db = produceDatabase(databaseFile);
		
		SneerAdmin admin = newSneerAdmin(newNetworkSimulator(), db);
		
		PrivateKey privateKey = admin.privateKey();
		
		dispose(db);
		
		assertEquals(privateKey, newSneerAdmin(newNetworkSimulator(), produceDatabase(databaseFile)).privateKey());
		
	}

	protected Object produceDatabase(File databaseFile) throws IOException {
		return ClojureUtils.var("sneer.core.tests.jdbc-tuple-base", "create-sqlite-db").invoke(databaseFile);
	}

	private SneerAdmin newSneerAdmin(Object network, Object db) {
		return (SneerAdmin) ClojureUtils.adminVar("new-sneer-admin-over-db").invoke(network, db);
	}

}
