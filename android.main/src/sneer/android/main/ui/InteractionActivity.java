package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.*;

import java.util.*;

import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.commons.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.support.v4.app.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

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
		
		Party party = SNEER.produceParty((PublicKey)getIntent().getExtras().getSerializable(PARTY_PUK));
		
		this.setTitle(SNEER.labelFor(party).mostRecent());

		SNEER.interactions().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<List<Interaction>>() { @Override public void call(List<Interaction> interactions) {
			for (Interaction interaction : interactions) {
				interaction.events().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<List<InteractionEvent>>() { @Override public void call(List<InteractionEvent> events) {
					for (InteractionEvent event : events) {
						onInteractionEvent(event);
					}
				}});
			}
		}});

		interactionAdapter = new InteractionAdapter(this,
			this.getLayoutInflater(),
			R.layout.list_item_user_message,
			R.layout.list_item_party_message,
			messages,
			SNEER);

		ListView listView = (ListView) findViewById(R.id.listView);
		listView.setAdapter(interactionAdapter);


		final Button b = (Button)findViewById(R.id.sendButton);
		b.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			SNEER.interactions().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<List<Interaction>>() { @Override public void call(List<Interaction> interactions) {
				final TextView widget = (TextView)findViewById(R.id.editText);
				for (Interaction interaction : interactions)
					interaction.sendMessage(widget.getText().toString());
				
				widget.setText("");
			}});
		}});
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
