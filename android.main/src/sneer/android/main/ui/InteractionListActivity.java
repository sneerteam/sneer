package sneer.android.main.ui;

import java.util.*;

import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.R;
import sneer.snapi.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.*;
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
 * {@link InteractionListFragment} and the item details (if present) is a
 * {@link InteractionDetailFragment}.
 * <p>
 */

//ChatListActivity -> Listar chats
public class InteractionListActivity extends FragmentActivity implements InteractionListFragment.Callbacks {

	private static InteractionListFragment interactionListFragment;

	/** Whether or not the activity is in two-pane mode, i.e. running on a tablet device. */
	private boolean mTwoPane;


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
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
		.subscribe(new Action1<Collection<Interaction>>() {
			@Override
			public void call(Collection<Interaction> interaction) {
//				IndividualSimulator member = new IndividualSimulator(Observable.from("0099f12"), Observable.from("joao"));
//
//				interaction.contact(member);
//				interactionListFragment.addInteraction(interaction);
			}
		});



		sneer().interactions().observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<Collection<Interaction>>() {
					@Override
					public void call(Collection<Interaction> interaction) {
						interactionListFragment.addInteraction(interaction);
					}
				});

		interactionListFragment = (InteractionListFragment) getSupportFragmentManager()
				.findFragmentById(R.id.chat_list);

		if (findViewById(R.id.interaction_detail_container) != null) {
			mTwoPane = true;
			interactionListFragment.setActivateOnItemClick(true);
		}



	}


	/** Callback method from {@link InteractionListFragment.Callbacks} indicating that the item with the given ID was selected. */
	@Override
	public void onItemSelected(Interaction interaction) {

		if (mTwoPane) {
			Bundle arguments = new Bundle();
			arguments.putString(InteractionDetailFragment.PARTY_PUK,
					interaction.party().publicKey().mostRecent().toString());
			InteractionDetailFragment fragment = new InteractionDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.interaction_detail_container, fragment).commit();

		} else {
			Intent detailIntent = new Intent(this, InteractionDetailActivity.class);
			detailIntent.putExtra(InteractionDetailFragment.PARTY_PUK,

					interaction.party().publicKey().mostRecent().toString());
			startActivity(detailIntent);
		}
	}


	private Sneer sneer() {
		return null;
//		return ((SneerApp) getApplication()).model();
	}


	void toast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}
}
