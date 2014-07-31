package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.*;

import java.util.*;

import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.android.main.ui.InteractionListActivity.EmbeddedOptions;
import sneer.commons.*;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

public class InteractionActivity extends Activity {

	static final String PARTY_PUK = "partyPuk";

	private static final Comparator<? super InteractionEvent> BY_TIMESTAMP = new Comparator<InteractionEvent>() { @Override public int compare(InteractionEvent lhs, InteractionEvent rhs) {
		return Comparators.compare(lhs.timestampSent(), rhs.timestampSent());
	}};

	private final List<InteractionEvent> messages = new ArrayList<InteractionEvent>();
	private InteractionAdapter adapter;

	private ActionBar actionBar;
	private EmbeddedOptions embeddedOptions;
	private Party party;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_interaction);
		
		actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setHomeButtonEnabled(true);
		
		embeddedOptions = (EmbeddedOptions) getIntent().getExtras().getSerializable("embeddedOptions");
		
		party = sneer().produceParty((PublicKey)getIntent().getExtras().getSerializable(PARTY_PUK));
		
		sneer().labelFor(party).observable().subscribe(new Action1<String>() { @Override public void call(String label) {
			actionBar.setTitle(label);
		}});
		
		sneer().profileFor(party).selfie().observeOn(AndroidSchedulers.mainThread()).cast(byte[].class).subscribe(new Action1<byte[]>() { @Override public void call(byte[] selfie) {
			actionBar.setIcon((Drawable)new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(selfie, 0, selfie.length)));
		}});

		sneer().produceInteractionWith(party).events().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<List<InteractionEvent>>() { @Override public void call(List<InteractionEvent> events) {
			messages.clear();
			messages.addAll(events);
			adapter.notifyDataSetChanged();
		}});

		adapter = new InteractionAdapter(this,
			this.getLayoutInflater(),
			R.layout.list_item_user_message,
			R.layout.list_item_party_message,
			messages,
			party,
			sneer());

		ListView listView = (ListView) findViewById(R.id.listView);
		listView.setAdapter(adapter);

		final Button b = (Button)findViewById(R.id.sendButton);
		final TextView editText = (TextView)findViewById(R.id.editText);
		b.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			sendMessage(party, editText.getText().toString().trim());
			editText.setText("");
		}

		private void sendMessage(final Party party, final String text) {
			if (text != null && !text.isEmpty())
				sneer().produceInteractionWith(party).sendMessage(text);
		}});		
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (embeddedOptions.interactionAction == null) {
			return false;
		}
		getMenuInflater().inflate(R.menu.interaction, menu);
		if (embeddedOptions.interactionLabel != null) {
			MenuItem item = menu.findItem(R.id.action_new_interaction);
			item.setTitle(embeddedOptions.interactionLabel);
		}
		return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_new_interaction:
			launchNewInteraction();
			return true;
		case android.R.id.home:
			navigateToProfile();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	private void navigateToProfile() {
		Intent intent = new Intent();
		intent.setClass(this, ProfileActivity.class);
		intent.putExtra("partyPuk", party.publicKey().mostRecent());
		startActivity(intent);
	}


	private void launchNewInteraction() {
		Intent intent = new Intent(embeddedOptions.interactionAction);
		intent.putExtra(SneerAndroid.TYPE, embeddedOptions.type);
		intent.putExtra("myPrivateKey", new ClientPrivateKey(sneer().self().publicKey().mostRecent()));
		intent.putExtra("contactNickname", sneer().findContact(party).nickname().mostRecent());
		intent.putExtra("contactPuk", party.publicKey().mostRecent());
		startActivity(intent);
	}
	
	
	@SuppressWarnings("unused")
	private void onInteractionEvent(InteractionEvent msg) {
		int insertionPointHint = Collections.binarySearch(messages, msg, BY_TIMESTAMP);
		if (insertionPointHint < 0) {
			int insertionPoint = Math.abs(insertionPointHint) - 1;
			messages.add(insertionPoint, msg);
			adapter.notifyDataSetChanged();
		}
	}
	
	
}
