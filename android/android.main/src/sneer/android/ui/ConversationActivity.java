package sneer.android.ui;

import static sneer.android.SneerAndroidSingleton.sneer;

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
import sneer.android.R;
import sneer.commons.Comparators;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;

public class ConversationActivity extends SneerActivity {

	public static final String PARTY_PUK = "partyPuk";
	private static final String ACTIVITY_TITLE = "activityTitle";

	private static final Comparator<? super Message> BY_TIMESTAMP = new Comparator<Message>() { @Override public int compare(Message lhs, Message rhs) {
		return Comparators.compare(lhs.timestampReceived(), rhs.timestampReceived());
	}};

	private final List<Message> messages = new ArrayList<Message>();
	private ConversationAdapter adapter;

	private ActionBar actionBar;
	private Party party;
	private Conversation conversation;

	private ImageButton actionButton;
	protected boolean justOpened;
	private EditText editText;

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setContentView(R.layout.activity_conversation);
		justOpened = true;
		actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(true);

		party = sneer().produceParty((PublicKey)getIntent().getExtras().getSerializable(PARTY_PUK));

		plugActionBarTitle(actionBar, party.name());
		plugActionBarIcon(actionBar, sneer().profileFor(party).selfie());

		conversation = sneer().produceConversationWith(party);
		conversation.setBeingRead(true);

		adapter = new ConversationAdapter(this,
			this.getLayoutInflater(),
			R.layout.list_item_user_message,
			R.layout.list_item_party_message,
			messages,
			party);

		deferUI(conversation.messages())
			.subscribe(new Action1<List<Message>>() { @Override public void call(List<Message> msgs) {
				messages.clear();
				messages.addAll(msgs);
				adapter.notifyDataSetChanged();
			}});

		((ListView)findViewById(R.id.messageList)).setAdapter(adapter);

		editText = (EditText) findViewById(R.id.editText);
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

		editText.setOnKeyListener(new OnKeyListener() { @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (!isHardwareKeyboardAvailable()) return false;
			if (!(event.getAction() == KeyEvent.ACTION_DOWN)) return false;
			if (!(keyCode == KeyEvent.KEYCODE_ENTER)) return false;
			handleClick(editText.getText().toString().trim());
			return true;
		}});

		actionButton = (ImageButton)findViewById(R.id.actionButton);
		actionButton.setImageResource(R.drawable.ic_action_new);
		actionButton.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			handleClick(editText.getText().toString().trim());
		}});
	}


	private void handleClick(String text) {
		if (!text.isEmpty())
			conversation.sendText(text);
		else
			openIteractionMenu();
		editText.setText("");
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
		}});
		menu.setOnDismissListener(new OnDismissListener() {  @Override public void onDismiss(PopupMenu menu) {
			s.unsubscribe();
		}});
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


	private void hideKeyboard() {
		if (justOpened) {
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
			InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
		}
		justOpened = false;
	}


	private boolean isHardwareKeyboardAvailable() {
		return getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
	}


	@Override
	protected void onPause() {
		super.onPause();
		conversation.setBeingRead(false);
	}


	@Override
	protected void onResume() {
		super.onResume();
		conversation.setBeingRead(true);
		hideKeyboard();
	}

}
