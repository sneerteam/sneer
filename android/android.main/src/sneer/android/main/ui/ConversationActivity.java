package sneer.android.main.ui;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static sneer.android.main.ui.SneerAndroidProvider.sneer;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import sneer.Conversation;
import sneer.ConversationMenuItem;
import sneer.Message;
import sneer.Party;
import sneer.PublicKey;
import sneer.android.main.R;
import sneer.android.ui.SneerActivity;
import sneer.commons.Comparators;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.TextView;

public class ConversationActivity extends SneerActivity {

	static final String PARTY_PUK = "partyPuk";
	static final String ACTIVITY_TITLE = "activityTitle";

	private static final Comparator<? super Message> BY_TIMESTAMP = new Comparator<Message>() { @Override public int compare(Message lhs, Message rhs) {
		return Comparators.compare(lhs.timestampCreated(), rhs.timestampCreated());
	}};

	private final List<Message> messages = new ArrayList<Message>();
	private ConversationAdapter adapter;

	private ActionBar actionBar;
	private Party party;
	private Conversation conversation;

	private ImageButton actionButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_conversation);
		
		actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setHomeButtonEnabled(true);
		
		party = sneer().produceParty((PublicKey)getIntent().getExtras().getSerializable(PARTY_PUK));
		
		plugActionBarTitle(actionBar, party.name());
		plugActionBarIcon(actionBar, sneer().profileFor(party).selfie());
//		Observable<byte[]> selfie = sneer().profileFor(party).selfie();
//		deferUI(selfie).subscribe(new Action1<byte[]>() { @Override public void call(byte[] selfie) {
//			actionBar.setIcon((Drawable)new BitmapDrawable(getResources(), BitmapFactory.decodeByteArray(selfie, 0, selfie.length)));
//		}});

		conversation = sneer().produceConversationWith(party);
		conversation.unreadMessageCountReset();

		adapter = new ConversationAdapter(this,
			this.getLayoutInflater(),
			R.layout.list_item_user_message,
			R.layout.list_item_party_message,
			messages,
			party);

		deferUI(conversation.messages().throttleLast(250, MILLISECONDS)) //Maybe the Android UI already does its own throttling? Try with and without this throttling and see if there is a difference.
			.subscribe(new Action1<List<Message>>() { @Override public void call(List<Message> msgs) {
				messages.clear();
				messages.addAll(msgs);
				adapter.notifyDataSetChanged();
			}});

		((ListView)findViewById(R.id.messageList)).setAdapter(adapter);

		final TextView editText = (TextView) findViewById(R.id.editText);
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (editText.getText().toString().trim() != null && !editText.getText().toString().trim().isEmpty())
					actionButton.setImageResource(R.drawable.ic_action_send);
				else
					actionButton.setImageResource(R.drawable.ic_action_new);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {}
		});
		
		actionButton = (ImageButton)findViewById(R.id.actionButton);
		actionButton.setImageResource(R.drawable.ic_action_new);
		actionButton.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			handleClick(editText.getText().toString().trim());
			editText.setText("");
		} });
	}
	
	private void handleClick(String text) {
		if (!text.isEmpty())
			conversation.sendMessage(text);
		else
			openIteractionMenu();
	}
	
	private void openIteractionMenu() {
		final PopupMenu menu = new PopupMenu(ConversationActivity.this, actionButton);
		
		final Subscription s = conversation.menu().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<List<ConversationMenuItem>>() {  @SuppressWarnings("deprecation")
		@Override public void call(List<ConversationMenuItem> menuItems) {
			menu.getMenu().close();
			menu.getMenu().clear();
			for (final ConversationMenuItem item : menuItems) {
				menu.getMenu().add(item.caption()).setOnMenuItemClickListener(new OnMenuItemClickListener() { @Override public boolean onMenuItemClick(MenuItem ignored) {
					menu.getMenu().close();
					item.call(party.publicKey().current());
					return true;
				}}).setIcon(new BitmapDrawable(BitmapFactory.decodeStream(new ByteArrayInputStream(item.icon()))));
			}
			menu.show();
		} });
		menu.setOnDismissListener(new OnDismissListener() {  @Override public void onDismiss(PopupMenu menu) {
			s.unsubscribe();
		} });
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			navigateToProfile();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	private void navigateToProfile() {
		Intent intent = new Intent();
		intent.setClass(this, ContactActivity.class);
		intent.putExtra(PARTY_PUK, party.publicKey().current());
		intent.putExtra(ACTIVITY_TITLE, "Contact");
		startActivity(intent);
	}


	@SuppressWarnings("unused")
	private void onMessage(Message msg) {
		int insertionPointHint = Collections.binarySearch(messages, msg, BY_TIMESTAMP);
		if (insertionPointHint < 0) {
			int insertionPoint = Math.abs(insertionPointHint) - 1;
			messages.add(insertionPoint, msg);
			adapter.notifyDataSetChanged();
		}
	}
	
}
