package sneer;

import rx.*;
import rx.android.schedulers.*;
import sneer.rx.*;
import sneer.tuples.*;
import sneer.utils.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class SneerAndroid {

// This is how one uses the startInteractionList() method to start Sneer with interactions filtered by tuple type:
//
//	@Override
//	protected void onResume() {
//		super.onResume();
//		
//		String title = "RPS Challenges";
//		String type = "rock-paper-scissors/move";
//		String newInteractionLabel = "Challenge!!";
//		String newInteractionAction = "sneer.tutorial.rockpaperscissors.CHALLENGE";
//		
//		SneerAndroid.startInteractionList(this, title, type, newInteractionLabel, newInteractionAction);
//	}

	
	public static final String TYPE = "type";

	private static final String INTERACTION_LIST = "sneer.android.main.INTERACTION_LIST";
	public static final String TITLE = "title";
	public static final String NEW_INTERACTION_LABEL = "newInteractionLabel";
	public static final String NEW_INTERACTION_ACTION = "newInteractionAction";
	public static final String DISABLE_MENUS = "disable-menus";
	static final String SNEER_SERVICE = "sneer.android.service.BACKEND";
	
	public static void startInteractionList(Activity activity, String title, String type, String newInteractionLabel, String newInteractionAction) {
		try {
			Intent intent = new Intent(INTERACTION_LIST);
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
		return new SneerAndroid(activity).session();
	}
	
	private Context context;

	static Object unbundle(Bundle resultData) {
		return resultData.get("value");
	}
	
	public SneerAndroid(Context context) {
		this.context = context;
	}
	
	@SuppressWarnings("unchecked")
	public <T> Session<T> session() {
		if (!(context instanceof Activity)) {
			throw new IllegalStateException("Context expected to be an Activity, found " + context.getClass().getName());
		}
		
		final PrivateKey myPrivateKey = (PrivateKey) getExtra("myPrivateKey");
		final TupleSpace tupleSpace = new TupleSpaceFactoryClient(context).newTupleSpace(myPrivateKey);
		
		return new Session<T>() {

			@Override
			public Observed<String> contactNickname() {
				return new Observed<String>() {
					
					@Override
					public Observable<String> observable() {
						throw new RuntimeException("not implemented yet");
					}
					
					@Override
					public String current() {
						return (String) getExtra("contactNickname");
					}
				};
			}

			@Override
			public void send(T value) {
				tupleSpace.publisher()
					.audience(partyPublicKey())
					.type(type())
					.pub(value);
			}

			private String type() {
				return (String) getExtra(TYPE);
			}

			@Override
			public Observable<T> received() {
				return (Observable<T>) tupleSpace.filter()
						.audience(myPrivateKey)
						.author(partyPublicKey())
						.type(type())
						.tuples()
						.observeOn(AndroidSchedulers.mainThread())
						.subscribeOn(AndroidSchedulers.mainThread())
						.map(Tuple.TO_PAYLOAD);
						
			}

			@Override
			public void dispose() {
				// TODO
			}
			
			private PublicKey partyPublicKey() {
				return (PublicKey) getExtra("contactPuk");
			}
			
		};
	}
	
	private Object getExtra(String key) {
		return ((Activity)context).getIntent().getExtras().get(key);
	}

}
