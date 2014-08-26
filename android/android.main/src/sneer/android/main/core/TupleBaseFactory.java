package sneer.android.main.core;

import java.io.*;

import sneer.*;
import clojure.lang.*;

public class TupleBaseFactory {

	public static Object openTupleBase(File file) throws IOException {
		return prepareTupleBase(SneerSqliteDatabase.openDatabase(file));
	}

	static Object prepareTupleBase(SneerSqliteDatabase db) {
		tupleBase("create-tuple-table").invoke(db);
		return tupleBase("create").invoke(db);
	}

	private static IFn tupleBase(String varName) {
		return ClojureUtils.var("sneer.persistent-tuple-base", varName);
	}

}
