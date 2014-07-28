package sneer;

public interface ObjectReplacer<LocalType, RemoteType> {
	
	RemoteType outgoing(LocalType local);
	LocalType incoming(RemoteType remote);

}
