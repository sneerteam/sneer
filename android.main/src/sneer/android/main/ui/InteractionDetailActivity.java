package sneer.android.main.ui;

import java.util.*;

import sneer.*;
import sneer.android.main.*;
import sneer.commons.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.view.*;

public class InteractionDetailActivity extends Activity {

	static final String PARTY_PUK = "partyPuk";

	private static final Comparator<? super InteractionEvent> BY_TIMESTAMP = new Comparator<InteractionEvent>() { @Override public int compare(InteractionEvent lhs, InteractionEvent rhs) {
		return Comparators.compare(lhs.timestampSent(), rhs.timestampSent());
	}};

	private final List<InteractionEvent> messages = new ArrayList<InteractionEvent>();
	private InteractionAdapter interactionAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_interaction_detail);

//		View rootView = inflater.inflate(R.layout.fragment_chat_detail,	container, false);
//
////		Sneer sneer = ((SneerApp) getActivity().getApplication()).model();
//		
////		Party party = sneer.produceParty(getArguments().getString(PARTY_PUK));
////		final Interaction interaction = sneer.produceInteractionWith(party);
//
//		this.setTitle(interaction.party().nickname().toBlockingObservable().first());
//
//		interaction.events().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<InteractionEvent>() { @Override public void call(InteractionEvent msg) {
//			onInteractionEvent(msg);
//		}});
//
//		interactionAdapter = new InteractionAdapter(
//			this.getActivity(),
//			inflater,
//			R.layout.list_item_user_message,
//			R.layout.list_item_party_message,
//			messages);
//
//		ListView listView = (ListView) rootView.findViewById(R.id.listView);
//		listView.setAdapter(interactionAdapter);
//
//		final TextView widget = (TextView) rootView.findViewById(R.id.editText);
//
//		Button b = (Button)rootView.findViewById(R.id.sendButton);
//		b.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
//			interaction.sendMessage(widget.getText().toString());
//			widget.setText("");
//		}});
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpTo(this, new Intent(this, InteractionListActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
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
