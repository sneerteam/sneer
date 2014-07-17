package sneer.android.main.ui.interactions;

import rx.*;
import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.android.main.R;
import sneer.simulator.*;
import sneer.snapi.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.util.*;
import android.view.*;
import android.widget.*;

/**
 * This activity has different
 * presentations for handset and tablet-size devices. On handsets, the activity
 * presents a list of items, which when touched, lead to a
 * {@link InteractionDetailActivity} representing item details. On tablets, the
 * activity presents the list of items and item details side-by-side using two
 * vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link InteractionsListFragment} and the item details (if present) is a
 * {@link InteractionDetailFragment}.
 * <p>
 */

//ChatListActivity -> Listar chats
public class InteractionsListActivity extends FragmentActivity implements InteractionsListFragment.Callbacks {

	private static InteractionsListFragment interactionsListFragment;

	/** Whether or not the activity is in two-pane mode, i.e. running on a tablet device. */
	private boolean mTwoPane;


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}


//	@Override
//	public boolean onOptionsItemSelected(MenuItem item) {
//		if (item.getItemId() == R.id.action_contacts)
//			chat().findParty().subscribe(new Action1<Party>() {
//				@Override
//				public void call(Party party) {
//					onItemSelected(chat().produceInteractionWith(party));
//				}
//			});
//
//		return true;
//	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat_list);

		SneerUtils.showSneerInstallationMessageIfNecessary(this);



		sneer().interactions().observeOn(AndroidSchedulers.mainThread())
		.subscribe(new Action1<Interaction>() {
			@Override
			public void call(Interaction interaction) {
				IndividualSimulator member = new IndividualSimulator(Observable.from("0099f12"), Observable.from("joao"));

				interaction.contact(member);
				interactionsListFragment.addInteraction(interaction);
			}
		});



		sneer().interactions().observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<Interaction>() {
					@Override
					public void call(Interaction interaction) {
						interactionsListFragment.addInteraction(interaction);
					}
				});

		interactionsListFragment = (InteractionsListFragment) getSupportFragmentManager()
				.findFragmentById(R.id.chat_list);

		if (findViewById(R.id.chat_detail_container) != null) {
			mTwoPane = true;
			interactionsListFragment.setActivateOnItemClick(true);
		}



	}


	protected void log(String string) {
		Log.d(InteractionsListActivity.class.getSimpleName(), string);
	}


	/** Callback method from {@link InteractionsListFragment.Callbacks} indicating that the item with the given ID was selected. */
	@Override
	public void onItemSelected(Interaction interaction) {

		if (mTwoPane) {
			Bundle arguments = new Bundle();
			arguments.putString(InteractionDetailFragment.PARTY_PUK,
					interaction.party().publicKey().toBlockingObservable().first());
			InteractionDetailFragment fragment = new InteractionDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.chat_detail_container, fragment).commit();

		} else {
			Intent detailIntent = new Intent(this, InteractionDetailActivity.class);
			detailIntent.putExtra(InteractionDetailFragment.PARTY_PUK,

					interaction.party().party().publicKey().toBlockingObservable().first());
			startActivity(detailIntent);
		}
	}


	private Sneer sneer() {
		return ((SneerApp) getApplication()).model();
	}


	void toast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}
}
