package core;

import sneer.admin.*;

public class GroupsGlue extends sneer.Groups {
	@Override
	protected SneerAdmin createSneerAdmin() {
		return Glue.newSneerAdmin();
	}
}