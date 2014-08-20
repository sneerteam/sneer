package sneer;

import java.io.*;

public interface PublicKey extends Serializable {

	String asBitcoinAddress();
	byte[] bytes();
	
}
