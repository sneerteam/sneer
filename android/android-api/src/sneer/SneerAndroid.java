package sneer;

import rx.*;
import sneer.commons.exceptions.*;
import sneer.tuples.*;
import sneer.utils.*;
import android.app.*;
import android.content.*;
import android.os.*;

public class SneerAndroid {

// This is how one uses the startConversationList() method to start Sneer with conversations filtered by tuple type:
//
//	@Override
//	protected void onResume() {
//		super.onResume();
//		
//		String title = "RPS Challenges";
//		String type = "rock-paper-scissors/move";
//		String newConversationLabel = "Challenge!!";
//		String newConversationAction = "sneer.tutorial.rockpaperscissors.CHALLENGE";
//		
//		SneerAndroid.startConversationList(this, title, type, newConversationLabel, newConversationAction);
//	}

	
	public static final String TYPE = "type";
	public static final String PARTY_PUK = "partyPuk";
	
	private static final String CONVERSATION_LIST = "sneer.android.main.ui.MAIN";
	public static final String TITLE = "title";
	public static final String NEW_CONVERSATION_LABEL = "newConversationLabel";
	public static final String NEW_CONVERSATION_ACTION = "newConversationAction";
	public static final String DISABLE_MENUS = "disable-menus";
	static final String SNEER_SERVICE = "sneer.android.service.BACKEND";

	public static final String SESSION_ID = "sessionId";

	
	public static void startMain(Activity activity, String title, String type, String newConversationLabel, String newConversationAction) {
		try {
			Intent intent = new Intent(CONVERSATION_LIST);
			intent.putExtra(TITLE, title);
			intent.putExtra(TYPE, type);
			intent.putExtra(NEW_CONVERSATION_LABEL, newConversationLabel);
			intent.putExtra(NEW_CONVERSATION_ACTION, newConversationAction);
			activity.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			SneerUtils.showInstallSneerDialog(activity);
		}
	}
	
	private Context context;

	static Object unbundle(Bundle resultData) {
		return resultData.get("value");
	}
	
	public SneerAndroid(Context context) {
		this.context = context;
	}
	
	public Session session(long id) {
		
		//These were the old args:
		final PublicKey peerPuk = null; final String type = null;

		if (null == null)
			throw new NotImplementedYet();

		
		PrivateKey thirdPartyAppShouldntNeedToKnowOwnPrivateKey = null; int letFixThis;
		final TupleSpace tupleSpace = new TupleSpaceFactoryClient(context).newTupleSpace(
			thirdPartyAppShouldntNeedToKnowOwnPrivateKey
		);
		
		return new Session() {

			@Override
			public void sendMessage(Object content) {
				tupleSpace.publisher()
					.audience(partyPuk())
					.type(type())
					.pub(content);
			}

			private String type() {
				return type;
			}

			@Override
			public void dispose() {
				// TODO
			}
			
			private PublicKey partyPuk() {
				return peerPuk;
			}

			@Override
			public Observable<String> peerName() {
				// TODO Auto-generated method stub
				throw new NotImplementedYet();
			}

			@Override
			public Observable<Message> previousMessages() {
				// TODO Auto-generated method stub
				throw new NotImplementedYet();
			}

			@Override
			public Observable<Message> newMessages() {
				// TODO Auto-generated method stub
				throw new NotImplementedYet();
			}
			
		};
	}

}
