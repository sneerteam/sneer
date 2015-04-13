package sneer.android.ui;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import me.sneer.R;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import sneer.Contact;
import sneer.Conversation;
import sneer.ConversationMenuItem;
import sneer.Message;
import sneer.Party;
import sneer.android.ui.adapters.ConversationAdapter;

import static rx.Observable.never;
import static sneer.android.SneerAndroidSingleton.sneer;
import static sneer.android.ui.ContactActivity.CURRENT_NICKNAME;
import static sneer.android.utils.Puk.shareOwnPublicKey;

public class ConversationActivity extends SneerActivity {

	private static final String ACTIVITY_TITLE = "activityTitle";

	private final List<Message> messages = new ArrayList<Message>();
	private ConversationAdapter adapter;

	private Conversation conversation;
	private Contact contact;

	private PopupMenu menu;
	private ImageButton actionButton;
	protected boolean justOpened;
	private EditText editText;
	private int icAction;
	private CompositeSubscription subscriptions;


	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setContentView(R.layout.activity_conversation);
		justOpened = true;
		ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setHomeButtonEnabled(true);

		contact = sneer().findByNick(getIntent().getStringExtra("nick"));

		plugActionBarTitle(actionBar, contact.nickname().observable());
		plugActionBarIcon(actionBar, selfieFor(contact));

		conversation = sneer().conversations().withContact(contact);

		adapter = new ConversationAdapter(this,
			this.getLayoutInflater(),
			R.layout.list_item_user_message,
			R.layout.list_item_party_message,
			messages,
			contact);

		((ListView)findViewById(R.id.messageList)).setAdapter(adapter);

		editText = (EditText) findViewById(R.id.editText);
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (!editText.getText().toString().trim().isEmpty())
					actionButton.setImageResource(R.drawable.ic_action_send);
				else
					actionButton.setImageResource(icAction);
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
		icAction = R.drawable.ic_action_send;

		actionButton.setImageResource(icAction);
		actionButton.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			handleClick(editText.getText().toString().trim());
		}});

		final TextView waiting = (TextView)findViewById(R.id.waitingMessage);
		final ListView messageList = (ListView)findViewById(R.id.messageList);
		String waitingMessage = this.getResources().getString(R.string.conversation_activity_waiting);
		waiting.setText(Html.fromHtml(String.format(waitingMessage, contact.nickname().current())));
		waiting.setMovementMethod(new LinkMovementMethod() {
			@Override
			public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
				shareOwnPublicKey(ConversationActivity.this, sneer().self(), contact.inviteCode(), contact.nickname().current());
				return true;
			}
		});

		contact.party().observable().subscribe(new Action1<Party>() {@Override public void call(Party party) {
			boolean enable = party != null;
			editText.setEnabled(enable);
			actionButton.setEnabled(enable);
			waiting.setVisibility(enable ? View.GONE : View.VISIBLE);
			messageList.setVisibility(enable ? View.VISIBLE : View.GONE);
		}});

		menu = new PopupMenu(ConversationActivity.this, actionButton);
	}

	private Observable<byte[]> selfieFor(Contact contact) {
		return contact.party().observable().switchMap(new Func1<Party, Observable<? extends byte[]>>() { @Override public Observable<? extends byte[]> call(Party party) {
			if (party == null)
				return never();
			else
				return sneer().profileFor(party).selfie();
		}});
	}

	private Message lastMessageReceived(List<Message> ms) {
		for (int i = ms.size() - 1; i >= 0; --i) {
			Message message = ms.get(i);
			if (!message.isOwn())
				return message;
		}
		return null;
	}


	private void handleClick(String text) {
		if (!text.isEmpty())
			conversation.sendMessage(text);
		else
			openInteractionMenu();
		editText.setText("");
	}


	private void openInteractionMenu() {
		menu.show();
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
		intent.putExtra(CURRENT_NICKNAME, contact.nickname().current());
		intent.putExtra(ACTIVITY_TITLE, "Contact");
		startActivity(intent);
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

		subscriptions.unsubscribe();

		sneer().conversations().notificationsStopIgnoring();
	}


	@Override
	protected void onResume() {
		super.onResume();

		hideKeyboard();

		sneer().conversations().notificationsStartIgnoring(conversation);

		subscriptions = new CompositeSubscription(
				subscribeToMessages(),
				subscribeToMenu());
	}

	private Subscription subscribeToMenu() {
		return conversation.menu().observeOn(AndroidSchedulers.mainThread()).subscribe(
				new Action1<List<ConversationMenuItem>>() {
					@SuppressWarnings("deprecation")
					@Override
					public void call(List<ConversationMenuItem> menuItems) {
						menu.getMenu().close();
						menu.getMenu().clear();
						if (menuItems.size() > 0) {
							icAction = R.drawable.ic_action_new;
							actionButton.setImageResource(icAction);
						} else {
							icAction = R.drawable.ic_action_send;
							actionButton.setImageResource(icAction);
						}

						for (final ConversationMenuItem item : menuItems) {
							menu.getMenu().add(item.caption()).setOnMenuItemClickListener(
									new OnMenuItemClickListener() {
										@Override
										public boolean onMenuItemClick(MenuItem ignored) {
											menu.getMenu().close();
											item.call(contact.party().current().publicKey().current());
											return true;
										}
									});
						}
					}
				});
	}

	private Subscription subscribeToMessages() {
		return deferUI(conversation.messages().debounce(200, TimeUnit.MILLISECONDS)).subscribe(new Action1<List<Message>>() {
			@Override
			public void call(List<Message> msgs) {
			messages.clear();
			messages.addAll(msgs);
			adapter.notifyDataSetChanged();
			Message last = lastMessageReceived(msgs);
			if (last != null)
				conversation.setRead(last);
			}
		});
	}

}
