package sneer.simulator;

import rx.*;
import sneer.*;

public class GroupSimulator implements Group {

	private Observable<Party> members;
	
	public GroupSimulator(){
		
	}
	
	@Override
	public Observable<String> publicKey() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Observable<String> name() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Observable<Party> members() {
		return members;
	}

}
