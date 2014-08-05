package sneer;

import rx.*;

public interface Profile {

	Observable<String> name();
	void setName(String string);
	
	Observable<String> preferredNickname();
	void setPreferredNickname(String newPreferredNickname);

	Observable<byte[]> selfie();
	void setSelfie(byte[] newSelfie);
	
	Observable<String> country();
	void setCountry(String newCountry);
	
	Observable<String> city();
	void setCity(String newCity);

}
