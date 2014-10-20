package sneer;

import clojure.java.api.*;
import clojure.lang.*;

public class ClojureUtils {

	public static IFn var(String ns, String simpleName) {
		Clojure.var("clojure.core/require").invoke(Clojure.read(ns));
		return Clojure.var(ns + "/" + simpleName);
	}

	public static IFn sneerCoreVar(String simpleName) {
		return var("sneer.core", simpleName);
	}

	public static IFn adminVar(String simpleName) {
		return var("sneer.admin", simpleName);
	}

	public static void dispose(Object disposable) {
		sneerCoreVar("dispose").invoke(disposable);
	}

}
