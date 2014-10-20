package sneer.admin;

import sneer.ClojureUtils;

public class SneerAdminFactory {
	
	public static SneerAdmin create(Object db) {
		return (SneerAdmin) ClojureUtils.adminVar("create").invoke(db);
	}
}
