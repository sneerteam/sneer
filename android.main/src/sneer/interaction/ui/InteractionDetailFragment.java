package sneer.interaction.ui;

import java.util.*;

import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import android.*;
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

	private static final Comparator<? super Message> BY_TIMESTAMP = new Comparator<Message>() { @Override public int compare(Message lhs, Message rhs) {
		return Comparators.compare(lhs.timestampSent(), rhs.timestampSent());
	}};

	private final List<Message> messages = new ArrayList<Message>();
	private InteractionAdapter interactionAdapter;

	
	/** Mandatory empty constructor for the fragment manager to instantiate the fragment (e.g. upon screen orientation changes). */
	public InteractionDetailFragment() {}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_chat_detail,	container, false);

		Chat chat = ((ChatApp) getActivity().getApplication()).model();
		
		Party party = chat.findParty(getArguments().getString(PARTY_PUK));
		final Conversation conversation = chat.produceConversationWith(party);

		getActivity().setTitle(conversation.party().nickname().toBlockingObservable().first());

		conversation.messages().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Message>() { @Override public void call(Message msg) {
			onMessage(msg);
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
			conversation.sendMessage(widget.getText().toString());
			widget.setText("");
		}});

		return rootView;
	}

	
	private void onMessage(Message msg) {
		int insertionPointHint = Collections.binarySearch(messages, msg, BY_TIMESTAMP);
		if (insertionPointHint < 0) {
			int insertionPoint = Math.abs(insertionPointHint) - 1;
			messages.add(insertionPoint, msg);
			interactionAdapter.notifyDataSetChanged();
		}
	}
	
}
