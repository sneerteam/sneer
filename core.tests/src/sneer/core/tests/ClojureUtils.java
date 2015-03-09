package sneer.core.tests;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

public class ClojureUtils {

	public static IFn var(String ns, String simpleName) {
		Clojure.var("clojure.core/require").invoke(Clojure.read(ns));
		return Clojure.var(ns + "/" + simpleName);
	}

	public static IFn adminVar(String simpleName) {
		return var("sneer.admin", simpleName);
	}
	
	public static void dispose(Object disposable) {
		var("sneer.commons", "dispose").invoke(disposable);
	}

}
