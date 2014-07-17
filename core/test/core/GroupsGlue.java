package core;

import sneer.admin.*;

public class GroupsGlue extends sneer.Groups {
	@Override
	protected SneerAdmin createSneerAdmin(Object session) {
		return Glue.newSneerAdmin(session);
	}
	
	@Override
	protected Object createSession() {
		return Glue.newSession();
	}
}