package sneer;

import java.io.*;

import rx.functions.*;

public interface PrivateKey extends Serializable {
	
	public static final Func1<PrivateKey, PublicKey> TO_PUBLIC_KEY = new Func1<PrivateKey, PublicKey>() {  @Override public PublicKey call(PrivateKey t1) {
		return t1.publicKey();
	}};
	
	PublicKey publicKey();

}
