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

public class SneerAdminImpl implements SneerAdmin {
	
	private TupleSpace tupleSpace;
	private final PrivateKey prik;
	
	private final class SneerImpl implements Sneer {
		@Override
		public TupleSpace tupleSpace() {
			return tupleSpace;
		}

		@Override
		public void addContact(String nickname, Party party) {
			throw new NotImplementedYet();

//			findContact(party).setNickname(nickname);
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
			final Contact contact = contacts.getUnchecked((WritablePartyImpl) party);
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
				public Observable<String> ownName() {
					return fields.get(String.class, "name");
				}
				
				@Override
				public void setOwnName(String name) {
					fields.pub("name", name);
				}
				
				@Override
				public void setSelfie(byte[] newSelfie) {
					if (newSelfie.length > 1024 * 10) throw new IllegalArgumentException("Selfie must be less than 10 kBytes. Was " + newSelfie.length + " bytes.");
					fields.pub("selfie", newSelfie);
				}
				
				@Override
				public void setPreferredNickname(String newPreferredNickname) {
					fields.pub("preferred-nickname", newPreferredNickname);
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
		public Conversation produceConversationWith(Party party) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Observable<String> nameFor(Party party) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Observable<List<Conversation>> conversationsContaining(String messageType) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Observable<List<Conversation>> conversations() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Contact findContact(Party party) {
//			return contacts.getUnchecked((WritablePartyImpl) party);
			throw new NotImplementedYet();
		}

		@Override
		public Observable<List<Contact>> contacts() {
			return null;
		}

		@Override
		public void setConversationMenuItems(List<ConversationMenuItem> menuItems) {
			// TODO Auto-generated method stub
			
		}

	}

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
		public T current() {
			return filter.localTuples()
					.map(Tuple.TO_PAYLOAD)
					.cast(clazz)
					.toBlocking()
					.last();
		}
	}
	
	class WritablePartyImpl implements Party {
		
		private final PartyKey key;
		private PublicKey puk;
		private Subscription pukSubscription;

		private WritablePartyImpl(PartyKey key) {
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
			
			tupleSpace.publisher()
				.audience(prik.publicKey())
				.type("sneer/publicKey")
				.field("party", key)
				.pub(puk);
			
		}
		
		public PartyKey key() {
			return key;
		}

		@Override
		public Observable<String> name() {
			throw new NotImplementedYet();
		}
	}

	class WritableContactImpl implements Contact {
		
		private final WritablePartyImpl party;

		public WritableContactImpl(WritablePartyImpl party) {
			this.party = party;
		}

		@Override
		public WritablePartyImpl party() {
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

		@Override
		public void setNickname(String newNickname) throws FriendlyException {
			newNickname = newNickname.trim();
			if (newNickname.equals(nickname().current())) return;
			
			String veto = problemWithNewNickname(newNickname);
			if (veto != null) throw new FriendlyException("Nickname " + veto + ".");
			
			tupleSpace.publisher()
				.audience(prik.publicKey())
				.type("sneer/profile.nickname")
				.field("party", party.key())
				.pub(newNickname);
		}

		@Override
		public String problemWithNewNickname(String newNick) {
			throw new NotImplementedYet();
//			if (newNick.trim().isEmpty()) return "cannot be empty";
//			Contact existing = sneer().findContact(party);
//			if (existing == this) return null;
//			if (existing == null) return null;
//			return "already used for another contact";
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
	
	private LoadingCache<PartyKey, WritablePartyImpl> parties = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<PartyKey, WritablePartyImpl>() {  @Override public WritablePartyImpl load(final PartyKey key) throws Exception {
		return new WritablePartyImpl(key);
	}});
	
	private LoadingCache<WritablePartyImpl, Contact> contacts = CacheBuilder.newBuilder().weakValues().build(new CacheLoader<WritablePartyImpl, Contact>() {  @Override public Contact load(final WritablePartyImpl party) throws Exception {
		return new WritableContactImpl(party);
	}});
	
	private SneerImpl sneer = new SneerImpl();


	public SneerAdminImpl(TupleSpace tupleSpace, PrivateKey ownPrik) {
		this.tupleSpace = tupleSpace;
		prik = ownPrik;
	}

	private WritablePartyImpl producePartyFromPuk(PublicKey puk) {
		WritablePartyImpl party = parties.getUnchecked(keys.getUnchecked(puk));
		party.setPublicKey(puk);
		return party;
	}

	@Override
	public SneerImpl sneer() {
		return sneer;
	}

	@Override
	public PrivateKey privateKey() {
		return prik;
	}

}
