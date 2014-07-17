package core;

import sneer.admin.*;
import clojure.java.api.*;
import clojure.lang.*;

class Glue {

	public static SneerAdmin newSneerAdmin(Object session) {
		try {
			return (SneerAdmin) sneerCoreVar("new-sneer-admin").invoke(session);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static Object newSession() {
		try {
			return sneerCoreVar("new-session").call();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	private static IFn sneerCoreVar(String simpleName) {
		Clojure.var("clojure.core/require").invoke(Clojure.read("sneer.core"));
		return Clojure.var("sneer.core/" + simpleName);
	}

}
