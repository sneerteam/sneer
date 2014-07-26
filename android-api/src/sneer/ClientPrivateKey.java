package sneer;

import java.io.*;

public class ClientPrivateKey implements PrivateKey, Serializable {
	private static final long serialVersionUID = 1L;

	private PublicKey publicKey;
	
	public ClientPrivateKey() {
	}

	public ClientPrivateKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}

	@Override
	public PublicKey publicKey() {
		return publicKey;
	}
	
	@Override
	public String toString() {
		return "ClientPrivateKey ["+publicKey+"]";
	}
	
}