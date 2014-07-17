package sneer;

import rx.Observable;


public interface Group extends Party {
	
	Observable<Party> members();
	
}
