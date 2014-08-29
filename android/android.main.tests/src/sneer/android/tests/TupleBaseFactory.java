package sneer.android.tests;

import java.io.*;

import sneer.*;
import sneer.android.main.core.*;
import clojure.lang.*;

public class TupleBaseFactory {

	public static Object openTupleBase(File file) throws IOException {
		return prepareTupleBase(SneerSqliteDatabase.openDatabase(file));
	}

	public static Object prepareTupleBase(SneerSqliteDatabase db) {
		tupleBase("create-tuple-table").invoke(db);
		return tupleBase("create").invoke(db);
	}

	private static IFn tupleBase(String varName) {
		return ClojureUtils.var("sneer.persistent-tuple-base", varName);
	}

	public static Object tempTupleBase() {
		try {
			return prepareTupleBase(SneerSqliteDatabase.openDatabase(File.createTempFile("self-test", "")));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
