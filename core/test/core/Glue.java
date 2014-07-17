package core;

import sneer.admin.*;
import clojure.java.api.*;

class Glue {

	public static SneerAdmin newSneerAdmin() {
		try {
			Clojure.var("clojure.core/require").invoke(Clojure.read("sneer.core"));
			return (SneerAdmin) Clojure.var("sneer.core/new-sneer-admin").call();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
