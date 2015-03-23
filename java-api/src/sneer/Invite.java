package sneer;

import sneer.rx.Observed;

public interface Invite {

	Observed<Long> code();

	Party party();

	void invalidate();

}
