package sneer;

import rx.*;
import sneer.snapi.*;
import sneer.tuples.*;
import android.app.*;
import android.content.*;

public class SneerAndroid {

	public static final String TITLE = "title";
	public static final String TYPE = "type";
	public static final String NEW_INTERACTION_LABEL = "newInteractionLabel";
	public static final String NEW_INTERACTION_ACTION = "newInteractionAction";

	public static void startInteractionList(Activity activity, String title, String type, String newInteractionLabel, String newInteractionAction) {
		try {
			Intent intent = new Intent("sneer.android.main.INTERACTION_LIST");
			intent.putExtra(TITLE, title);
			intent.putExtra(TYPE, type);
			intent.putExtra(NEW_INTERACTION_LABEL, newInteractionLabel);
			intent.putExtra(NEW_INTERACTION_ACTION, newInteractionAction);
			activity.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			SneerUtils.showInstallSneerDialog(activity);
		}
	}
	
	public static <T> Session<T> sessionOnAndroidMainThread(Activity activity) {
		return new SneerAndroid(activity).getSession();
	}
	
	private Context context;
	public static final String DISABLE_MENUS = "disable-menus";
	
	public SneerAndroid(Context context) {
		this.context = context;
	}
	
	@SuppressWarnings("unchecked")
	public <T> Session<T> getSession() {
		if (!(context instanceof Activity)) {
			throw new IllegalStateException("Context expected to be an Activity, found " + context.getClass().getName());
		}
		
		return new Session<T>() {

			private Contact contact;
			@Override
			public Contact contact() {
				if (contact == null) {
					contact = sneer().findContact(sneer().produceParty(partyPublicKey()));
				}
				return contact;
			}

			@Override
			public void send(T value) {
				sneer().tupleSpace().publisher()
					.audience(contact().party().publicKey().mostRecent())
					.type(type())
					.pub(value);
			}

			private String type() {
				return (String) getExtra(TYPE);
			}

			@Override
			public Observable<T> received() {
				return (Observable<T>) sneer().tupleSpace().filter()
						.audience(myPrivateKey())
						.author(contact().party().publicKey().mostRecent())
						.type(type())
						.tuples()
						.map(Tuple.TO_PAYLOAD);
						
			}

			@Override
			public void dispose() {
				throw new RuntimeException("Not implemented yet.");
			}
			private PublicKey partyPublicKey() {
				return (PublicKey) getExtra("partyPuk");
			}
			
		};
	}
	
//	public static class ClientPrivateKey implements PrivateKey, Parcelable {
//		
//		private PublicKey publicKey;
//		private String privateKey;
//
//		private ClientPrivateKey(PublicKey publicKey) {
//			this.publicKey = publicKey;
//		}
//
//		@Override
//		public int describeContents() {
//			return 0;
//		}
//
//		@Override
//		public void writeToParcel(Parcel dest, int flags) {
//			dest.writeSerializable(publicKey);
//		}
//
//		@Override
//		public PublicKey publicKey() {
//			return publicKey;
//		}
//		
//		public static final Parcelable.Creator<ClientPrivateKey> CREATOR = new Parcelable.Creator<ClientPrivateKey>() {
//			public ClientPrivateKey createFromParcel(Parcel in) {
//				return new ClientPrivateKey((PublicKey) in.readSerializable());
//			}
//			
//			public ClientPrivateKey[] newArray(int size) {
//				return new ClientPrivateKey[size];
//			}
//		};
//		
//	}

	protected PrivateKey myPrivateKey() {
		throw new RuntimeException("Not implemented yet.");
	}

	private Sneer sneer() {
		throw new RuntimeException("Not implemented yet.");
	}

	private Object getExtra(String key) {
		return ((Activity)context).getIntent().getExtras().get(key);
	}

}
