package sneer;

public interface Sneer {

	KeyPair newKeyPair();

	Cloud newCloud(KeyPair identity);

}
