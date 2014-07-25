package sneer;

import rx.*;
import sneer.commons.exceptions.*;

public interface Profile {

	Observable<String> preferredNickname();
	void setPreferredNickname(String newPreferredNickname);

	Observable<byte[]> selfie();
	void setSelfie(byte[] newSelfie) throws FriendlyException;
	
	Observable<String> country();
	void setCountry(String newCountry);
	
	Observable<String> city();
	void setCity(String newCity);

}
