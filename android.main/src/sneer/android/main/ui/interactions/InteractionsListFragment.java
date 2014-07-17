package sneer.android.main.ui.interactions;

import java.util.*;

import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.commons.*;
import sneer.util.*;
import android.app.*;
import android.os.*;
import android.support.v4.app.ListFragment;
import android.view.*;
import android.widget.*;


/**
 * A list fragment representing a list of Chats. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link InteractionDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class InteractionsListFragment extends ListFragment {

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	private static final Comparator<? super Interaction> BY_TIMESTAMP = new Comparator<Interaction>() { @Override public int compare(Interaction lhs, Interaction rhs) {
		return Comparators.compare(lhs.mostRecentEventTimestamp(), rhs.mostRecentEventTimestamp());
	}};
	
	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sDummyCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;
	
	
	
	private List<Interaction> interactions = new ArrayList<Interaction>();
	private ArrayAdapter<Interaction> interactionsAdapter;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(Interaction interaction);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onItemSelected(Interaction convesation) {
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public InteractionsListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		interactionsAdapter = new ArrayAdapter<Interaction>(getActivity(),
				android.R.layout.simple_list_item_activated_1,
				android.R.id.text1, interactions);
		setListAdapter(interactionsAdapter);
		
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null
				&& savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			int position = savedInstanceState
							.getInt(STATE_ACTIVATED_POSITION);
			getListView().setItemChecked(position, position != ListView.INVALID_POSITION);
			
			mActivatedPosition = position;
		}
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		mCallbacks.onItemSelected(interactions.get(position));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}


	
	
	public void addInteraction(Interaction interaction) {
		if (!interactions.contains(interaction)) {
			interactions.add(interaction);
			interaction.events().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<InteractionEvent>() {@Override public void call(InteractionEvent event) {
				Collections.sort(interactions, BY_TIMESTAMP);
				interactionsAdapter.notifyDataSetChanged();
			}});
		}
	}
	

	
}
