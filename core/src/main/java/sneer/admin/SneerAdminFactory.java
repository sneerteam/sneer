package sneer.admin;

import clojure.java.api.Clojure;

public class SneerAdminFactory {
	
	public static SneerAdmin create(Object db) {
        Clojure.var("clojure.core/require").invoke(Clojure.read("sneer.main"));
        return (SneerAdmin) Clojure.var("sneer.main" + "/" + "start").invoke(db);
	}
}
