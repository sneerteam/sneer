package sneer;

import rx.*;

public interface Profile {

	Observable<String> ownName();
	void setOwnName(String newOwnName);
	
	Observable<String> preferredNickname();
	void setPreferredNickname(String newPreferredNickname);

	Observable<byte[]> selfie();
	void setSelfie(byte[] newSelfie);
	
	Observable<String> country();
	void setCountry(String newCountry);
	
	Observable<String> city();
	void setCity(String newCity);

	/** A hack to determine whether the own name has already been filled in by the user or not (first time using Sneer) without resorting to timeouts on observables. */
//	boolean isOwnNameLocallyAvailable();
}
