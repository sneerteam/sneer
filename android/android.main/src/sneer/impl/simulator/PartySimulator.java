package sneer.impl.simulator;

import static sneer.Contact.*;
import static sneer.commons.Streams.*;
import static sneer.commons.exceptions.Exceptions.*;

import java.io.*;
import java.util.concurrent.*;

import rx.*;
import rx.functions.*;
import sneer.*;
import sneer.rx.*;


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
		this.publicKey = ObservedSubject.create(puk);
		this.isSelf = isSelf;
	}


	@Override
	public Observed<PublicKey> publicKey() {
		return publicKey.observed();
	}

	
	/////////////////////// Profile

	@Override
	public Observable<String> ownName() {
		return Observable.interval(3, TimeUnit.SECONDS).map(new Func1<Long, String>() { @Override public String call(Long t1) {
			return "ownName " + t1;
		}});
	}

	
	@Override
	public void setOwnName(String newName) {
		check(isSelf);
		System.out.println("================> setOwnName() called");
	}

	
	@Override
	public Observable<String> preferredNickname() {
		return Observable.interval(3, TimeUnit.SECONDS).map(new Func1<Long, String>() { @Override public String call(Long t1) {
			return "preferredNickname " + t1;
		}});
	}

	@Override
	public void setPreferredNickname(String newPreferredNickname) {
		check(isSelf);
		System.out.println("================> setPreferredNickname() called");
	}


	@Override
	public Observable<byte[]> selfie() {
		return Observable.interval(3, TimeUnit.SECONDS).map(new Func1<Long, byte[]>() { @Override public byte[] call(Long t1) {
			String file;
			boolean isEven = t1 % 2 == 0;
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
		return Observable.interval(3, TimeUnit.SECONDS).map(new Func1<Long, String>() { @Override public String call(Long t1) {
			return "city " + t1;
		}});
	}


	@Override
	public void setCity(String newCity) {
		check(isSelf);
		System.out.println("================> setCity() called");
	}

	
	@Override
	public Observable<String> country() {
		return Observable.interval(3, TimeUnit.SECONDS).map(new Func1<Long, String>() { @Override public String call(Long t1) {
			return "country " + t1;
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
