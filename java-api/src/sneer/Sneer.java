package sneer;

public interface Sneer {

	PrivateKey createPrivateKey();

	Cloud newCloud(PrivateKey identity);

}
