package sneer.android.main.ui.interactions;

import java.util.*;

import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.commons.*;
import sneer.util.*;
import android.R;
import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

/**
 * A fragment representing a single Chat detail screen. This fragment is either
 * contained in a {@link InteractionsListActivity} in two-pane mode (on tablets) or a
 * {@link InteractionDetailActivity} on handsets.
 */
public class InteractionDetailFragment extends Fragment {
	
	static final String PARTY_PUK = "partyPuk";

	private static final Comparator<? super InteractionEvent> BY_TIMESTAMP = new Comparator<InteractionEvent>() { @Override public int compare(InteractionEvent lhs, InteractionEvent rhs) {
		return Comparators.compare(lhs.timestampSent(), rhs.timestampSent());
	}};

	private final List<InteractionEvent> messages = new ArrayList<InteractionEvent>();
	private InteractionAdapter interactionAdapter;

	
	/** Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes). */
	public InteractionDetailFragment() {}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_chat_detail,	container, false);

		Sneer sneer = ((SneerApp) getActivity().getApplication()).model();
		
		Party party = sneer.produceParty(getArguments().getString(PARTY_PUK));
		final Interaction interaction = sneer.produceInteractionWith(party);

		getActivity().setTitle(interaction.party().nickname().toBlockingObservable().first());

		interaction.events().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<InteractionEvent>() { @Override public void call(InteractionEvent msg) {
			onInteractionEvent(msg);
		}});

		interactionAdapter = new InteractionAdapter(
			this.getActivity(),
			inflater,
			R.layout.list_item_user_message,
			R.layout.list_item_contact_message,
			messages);

		ListView listView = (ListView) rootView.findViewById(R.id.listView);
		listView.setAdapter(interactionAdapter);

		final TextView widget = (TextView) rootView.findViewById(R.id.editText);

		Button b = (Button)rootView.findViewById(R.id.sendButton);
		b.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			interaction.sendMessage(widget.getText().toString());
			widget.setText("");
		}});

		return rootView;
	}

	
	private void onInteractionEvent(InteractionEvent msg) {
		int insertionPointHint = Collections.binarySearch(messages, msg, BY_TIMESTAMP);
		if (insertionPointHint < 0) {
			int insertionPoint = Math.abs(insertionPointHint) - 1;
			messages.add(insertionPoint, msg);
			interactionAdapter.notifyDataSetChanged();
		}
	}
	
}
