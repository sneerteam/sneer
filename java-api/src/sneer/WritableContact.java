package sneer;

import sneer.commons.exceptions.*;

public interface WritableContact extends Contact {

	/** @return null if the new nickname is ok or a reason why the new nickname is not ok. */
	String problemWithNewNickname(String newNick);

	void setNickname(String newNick) throws FriendlyException;
	
}
