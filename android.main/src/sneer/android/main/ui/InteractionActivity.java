package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.SNEER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.InteractionEvent;
import sneer.Party;
import sneer.android.main.R;
import sneer.android.main.SneerSingleton;
import sneer.commons.Comparators;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class InteractionActivity extends Activity {

	static final String PARTY_PUK = "partyPuk";

	private static final Comparator<? super InteractionEvent> BY_TIMESTAMP = new Comparator<InteractionEvent>() { @Override public int compare(InteractionEvent lhs, InteractionEvent rhs) {
		return Comparators.compare(lhs.timestampSent(), rhs.timestampSent());
	}};

	private final List<InteractionEvent> messages = new ArrayList<InteractionEvent>();
	private InteractionAdapter interactionAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_interaction);
		
//		SNEER.produceParty(getIntent().getExtras().getSerializable(PARTY_PUK));
//		
//		this.setTitle(SNEER.interactions());
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
//		ListView listView = (ListView) findViewById(R.id.listView);
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
