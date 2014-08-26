package sneer.android.main.core;

import static sneer.ClojureUtils.*;
import static sneer.android.main.core.TupleBaseFactory.*;

import java.io.*;

import rx.*;
import rx.functions.*;
import sneer.*;
import sneer.commons.*;
import sneer.impl.keys.*;
import sneer.tuples.*;
import clojure.lang.*;

public class SneerTestUtils {

	private static void trySelfTest() throws IOException {
		File databaseFile = createTempFile();
		TupleSpace ts = createTupleSpace(networkSimulator("new-network").invoke(), databaseFile);
		
		ts.publisher().type("self-test").pub("42");
		ts.filter().type("self-test").tuples().subscribe(new Action1<Tuple>() {  @Override public void call(Tuple tuple) {
			System.out.println(tuple);
			SystemReport.updateReport("self-test.tuple", tuple);
		}},
		new Action1<Throwable>() {  @Override public void call(Throwable th) {
			report(th);	
		}});
	}

	public static void selfTest() {
		try {
			trySelfTest();
		} catch (Throwable e) {
			report(e);
		}
	}

	private static IFn networkSimulator(String var) {
		return var("sneer.networking.simulator", var);
	}

	private static TupleSpace createTupleSpace(Object network, File databaseFile) throws IOException {
		PublicKey puk = Keys.createPrivateKey().publicKey();
		
		Object tupleBase = openTupleBase(databaseFile);
		
		Object connection = sneerCoreVar("connect").invoke(network, puk);
		Object followees = Observable.never();
		TupleSpace ts = (TupleSpace)sneerCoreVar("reify-tuple-space").invoke(puk, tupleBase, connection, followees);
		return ts;
	}

	private static File createTempFile() throws IOException {
		return File.createTempFile("self-test", "");
	}

	private static void report(Throwable th) {
		th.printStackTrace();
		SystemReport.updateReport("self-test.error", th);
	}

	public static Object tmpTupleBase() {
		try {
			return prepareTupleBase(SneerSqliteDatabase.openDatabase(createTempFile()));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
