package sneer;

import java.io.*;
import java.util.*;

import rx.*;
import rx.Observable;
import sneer.admin.*;
import sneer.commons.exceptions.*;
import sneer.rx.*;
import sneer.tuples.*;

import com.google.common.cache.*;

public class TupleSpaceSneerAdmin implements SneerAdmin {
	
	private TupleSpace tupleSpace;
	private PrivateKey prik;
	
	class ObservedImpl<T> implements Observed<T> {
		
		private TupleFilter filter;
		private Class<T> clazz;

		public ObservedImpl(Class<T> clazz, TupleFilter filter) {
			this.clazz = clazz;
			this.filter = filter;
		}

		@Override
		public Observable<T> observable() {
			return filter.tuples()
					.map(Tuple.TO_PAYLOAD)
					.cast(clazz);
		}
		
		@Override
		public T mostRecent() {
			return filter.localTuples()
					.map(Tuple.TO_PAYLOAD)
					.cast(clazz)
					.toBlockingObservable()
					.last();
		}
	}
	
	class WritableParty implements Party {
		
		private final PartyKey key;
		private PublicKey puk;
		private Subscription pukSubscription;

		private WritableParty(PartyKey key) {
			this.key = key;
		}

		@Override
		public Observed<PublicKey> publicKey() {
			return new ObservedImpl<PublicKey>(PublicKey.class, tupleSpace.filter()
					.audience(prik)
					.author(prik.publicKey())
					.type("sneer/publicKey")
					.field("party", key));
		}
		
		private void setPublicKey(PublicKey puk) {
			if (puk.equals(this.puk)) {
				return;
			}
			this.puk = puk;
			
			if (pukSubscription != null) {
				pukSubscription.unsubscribe();
			}
			
			pukSubscription = tupleSpace.filter()
				.author(puk)
				.type("sneer/publicKey")
				.tuples()
				.map(Tuple.TO_PAYLOAD)
				.cast(PublicKey.class)
				.subscribe(tupleSpace.publisher()
						.audience(prik.publicKey())
						.type("sneer/publicKey")
						.field("party", key));
			
		}
		
		public PartyKey key() {
			return key;
		}
	}

	class WritableContact implements Contact {
		
		private final WritableParty party;

		public WritableContact(WritableParty party) {
			this.party = party;
		}

		@Override
		public WritableParty party() {
			return party;
		}
		
		@Override
		public Observed<String> nickname() {
			return new ObservedImpl<String>(String.class, tupleSpace.filter()
					.audience(prik)
					.author(prik.publicKey())
					.type("sneer/profile.nickname")
					.field("party", party.key()));
		}

		public void setNickname(String newNickname) {
			tupleSpace.publisher()
				.audience(prik.publicKey())
				.type("sneer/profile.nickname")
				.field("party", party.key())
				.pub(newNickname);
		}
	}
	
	public static class PartyKey implements Serializable {
		private static final long serialVersionUID = 1L;

		public PartyKey() {
		}
	}
	
	private LoadingCache<PublicKey, PartyKey> keys = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<PublicKey, PartyKey>() {  @Override public PartyKey load(final PublicKey key) throws Exception {
		return new PartyKey();
	}});
	
	private LoadingCache<PartyKey, WritableParty> parties = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<PartyKey, WritableParty>() {  @Override public WritableParty load(final PartyKey key) throws Exception {
		return new WritableParty(key);
	}});
	
	private LoadingCache<WritableParty, WritableContact> contacts = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<WritableParty, WritableContact>() {  @Override public WritableContact load(final WritableParty party) throws Exception {
		return new WritableContact(party);
	}});

	public TupleSpaceSneerAdmin(TupleSpace tupleSpace) {
		this.tupleSpace = tupleSpace;
	}

	@Override
	public void initialize(PrivateKey prik) {
		this.prik = prik;
	}

	@Override
	public PrivateKey privateKey() {
		return prik;
	}

	private WritableParty producePartyFromPuk(PublicKey puk) {
		WritableParty party = parties.getUnchecked(keys.getUnchecked(puk));
		party.setPublicKey(puk);
		return party;
	}

	@Override
	public Sneer sneer() {
		return new Sneer() {
			
			@Override
			public TupleSpace tupleSpace() {
				return tupleSpace;
			}
			
			@Override
			public void setContact(String nickname, Party party) throws FriendlyException {
				findContact(party).setNickname(nickname);
			}
			
			@Override
			public Party self() {
				return produceParty(prik.publicKey());
			}
			
			@Override
			public Profile profileFor(Party party) {
				if (party.publicKey() != prik.publicKey()) {
					throw new IllegalArgumentException("Editing someone else's profile is unsupported.");
				}
				final WritableContact contact = contacts.getUnchecked((WritableParty) party);
				class PublishableContact {
					void pub(String field, Object value) {
						tupleSpace.publisher()
							.audience(prik.publicKey())
							.type("sneer/profile." + field)
							.field("party", contact.party())
							.pub(value);
					}

					public <T> Observable<T> get(Class<T> clazz, String field) {
						return tupleSpace.filter()
								.author(prik.publicKey())
								.audience(prik)
								.type("sneer/profile."+field)
								.field("party", contact.party())
								.tuples()
								.map(Tuple.TO_PAYLOAD)
								.cast(clazz);
					}
					
				}
				final PublishableContact fields = new PublishableContact();
				return new Profile() {
					
					@Override
					public Observable<String> name() {
						return fields.get(String.class, "name");
					}
					
					@Override
					public void setName(String name) {
						fields.pub("name", name);
					}
					
					@Override
					public void setSelfie(byte[] newSelfie) throws FriendlyException {
						fields.pub("selfie", newSelfie);
					}
					
					@Override
					public void setPreferredNickname(String newPreferredNickname) {
						contact.setNickname(newPreferredNickname);
					}
					
					@Override
					public void setCountry(String newCountry) {
						fields.pub("country", newCountry);
					}
					
					@Override
					public void setCity(String newCity) {
						fields.pub("city", newCity);
					}
					
					@Override
					public Observable<byte[]> selfie() {
						return fields.get(byte[].class, "selfie");
					}
					
					@Override
					public Observable<String> preferredNickname() {
						return contact.nickname().observable();
					}
					
					@Override
					public Observable<String> country() {
						return fields.get(String.class, "country");
					}
					
					@Override
					public Observable<String> city() {
						return fields.get(String.class, "city");
					}
				};
			}
			
			@Override
			public Party produceParty(PublicKey publicKey) {
				return producePartyFromPuk(publicKey);
			}
			
			@Override
			public Interaction produceInteractionWith(Party party) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Observed<String> labelFor(Party party) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Observable<List<Interaction>> interactionsContaining(String eventType) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public Observable<List<Interaction>> interactions() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public WritableContact findContact(Party party) {
				return contacts.getUnchecked((WritableParty) party);
			}
			
			@Override
			public Observable<List<Contact>> contacts() {
				return null;
			}
		};
	}

}
