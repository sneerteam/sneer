package sneer.impl.simulator;

import static sneer.Contact.TO_NICKNAME;
import static sneer.commons.Streams.readFully;
import static sneer.commons.exceptions.Exceptions.check;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func1;
import sneer.Contact;
import sneer.Party;
import sneer.Profile;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.rx.Observed;
import sneer.rx.ObservedSubject;


public class PartySimulator implements Party, Profile {

	private final ObservedSubject<PublicKey> publicKey;
	private final boolean isSelf;

	/** Profile */
	private Sneer sneer;


	PartySimulator(PublicKey puk) {
		this(puk, true);
	}


	PartySimulator(PublicKey puk, Sneer sneer) {
		this(puk, false);
		this.sneer = sneer;
	}


	PartySimulator(PublicKey puk, boolean isSelf) {
		publicKey = ObservedSubject.create(puk);
		this.isSelf = isSelf;
	}


	@Override
	public Observed<PublicKey> publicKey() {
		return publicKey.observed();
	}


	/////////////////////// Profile

	@Override
	public Observable<String> ownName() {
		return Observable.interval(3, TimeUnit.SECONDS).map(new Func1<Long, String>() { @Override public String call(Long interval) {
			return "ownName " + interval;
		}});
	}


	@Override
	public void setOwnName(String newName) {
		check(isSelf);
		System.out.println("================> setOwnName() called");
	}


	@Override
	public Observable<String> preferredNickname() {
		return Observable.interval(3, TimeUnit.SECONDS).map(new Func1<Long, String>() { @Override public String call(Long interval) {
			return "preferredNickname " + interval;
		}});
	}

	@Override
	public void setPreferredNickname(String newPreferredNickname) {
		check(isSelf);
		System.out.println("================> setPreferredNickname() called");
	}


	@Override
	public Observable<byte[]> selfie() {
		return Observable.interval(3, TimeUnit.SECONDS).map(new Func1<Long, byte[]>() { @Override public byte[] call(Long interval) {
			String file;
			boolean isEven = interval % 2 == 0;
			if (isEven)
				file = "selfie_001.png";
			else
				file = "selfie_002.png";

			return selfieFromFileSystem(file);
		}});
	}


	@Override
	public void setSelfie(byte[] newSelfie) {
		check(isSelf);
		System.out.println("================> setSelfie() called");
	}


	@Override
	public Observable<String> city() {
		return Observable.interval(3, TimeUnit.SECONDS).map(new Func1<Long, String>() { @Override public String call(Long interval) {
			return "city " + interval;
		}});
	}


	@Override
	public void setCity(String newCity) {
		check(isSelf);
		System.out.println("================> setCity() called");
	}


	@Override
	public Observable<String> country() {
		return Observable.interval(3, TimeUnit.SECONDS).map(new Func1<Long, String>() { @Override public String call(Long interval) {
			return "country " + interval;
		}});
	}


	@Override
	public void setCountry(String newCountry) {
		check(isSelf);
		System.out.println("================> setCoutry() called");
	}


	@Override
	public Observable<String> name() {
		return ownName().first().concatWith(nickname());
	}


	private Observable<String> nickname() {
		return sneer.contacts().map(new Func1<Object, Contact>() { @Override public Contact call(Object ignored) {
			return findContact();
		}}).filter(new Func1<Contact, Boolean>() { @Override public Boolean call(Contact found) {
			return found != null;
		}}).flatMap(TO_NICKNAME);
	}


	private Contact findContact() {
		return sneer.findContact(this);
	}


	private byte[] selfieFromFileSystem(String fileName) {
		byte[] ret = null;
		try {
			ret = readFully(getClass().getResourceAsStream(fileName));
		} catch (IOException e) {}
		return ret;
	}


	@Override
	public boolean isOwnNameLocallyAvailable() {
		return true;
	}
}
