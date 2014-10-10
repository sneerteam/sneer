package sneer.core.tests;

import static sneer.ClojureUtils.dispose;
import static sneer.core.tests.Glue.newNetworkSimulator;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import sneer.ClojureUtils;
import sneer.PrivateKey;
import sneer.admin.SneerAdmin;

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
