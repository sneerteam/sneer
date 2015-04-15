package sneer.android.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import me.sneer.R;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.Contact;
import sneer.Conversation;
import sneer.Message;
import sneer.Party;
import sneer.android.ipc.InstalledPlugins;
import sneer.android.ipc.Plugin;
import sneer.android.ipc.PluginActivities;
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

	private ImageButton messageButton;
	private EditText messageInput;
	private Subscription subscription;


	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setContentView(R.layout.activity_conversation);
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

		messageInput = (EditText) findViewById(R.id.editText);
		messageInput.addTextChangedListener(new TextWatcher() { @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
				messageButton.setImageResource( messageInput.getText().toString().trim().isEmpty()
					? R.drawable.ic_action_new
					: R.drawable.ic_action_send);
			}

			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void afterTextChanged(Editable s) {}
		});

		messageInput.setOnKeyListener(new OnKeyListener() { @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (!isHardwareKeyboardAvailable()) return false;
			if (!(event.getAction() == KeyEvent.ACTION_DOWN)) return false;
			if (!(keyCode == KeyEvent.KEYCODE_ENTER)) return false;
			handleClick(messageInput.getText().toString().trim());
			return true;
		}});

		messageButton = (ImageButton)findViewById(R.id.actionButton);

		messageButton.setImageResource(R.drawable.ic_action_new);
		messageButton.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			handleClick(messageInput.getText().toString().trim());
		}});

		final TextView waiting = (TextView)findViewById(R.id.waitingMessage);
		final ListView messageList = (ListView)findViewById(R.id.messageList);
		String waitingMessage = this.getResources().getString(R.string.conversation_activity_waiting);
		waiting.setText(Html.fromHtml(String.format(waitingMessage, contact.nickname().current())));
		waiting.setMovementMethod(new LinkMovementMethod() { @Override public boolean onTouchEvent(@NonNull TextView widget, @NonNull Spannable buffer, @NonNull MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP)
				shareOwnPublicKey(ConversationActivity.this, sneer().self(), contact.inviteCode(), contact.nickname().current());
			return true;
		}});

		conversation.canSendMessages().subscribe(new Action1<Boolean>() {@Override public void call(Boolean canSendMessages) {
			messageInput .setEnabled(canSendMessages);
			messageButton.setEnabled(canSendMessages);
			waiting    .setVisibility(canSendMessages ? View.GONE : View.VISIBLE);
			messageList.setVisibility(canSendMessages ? View.VISIBLE : View.GONE);
		}});
	}

	private Observable<byte[]> selfieFor(Contact contact) {
		return contact.party().observable().switchMap(new Func1<Party, Observable<? extends byte[]>>() { @Override public Observable<? extends byte[]> call(Party party) {
			if (party == null)
				return never();
			else
				return sneer().profileFor(party).selfie();
		}});
	}

	private void handleClick(String text) {
		if (!text.isEmpty())
			conversation.sendMessage(text);
		else
			openInteractionMenu();
		messageInput.setText("");
	}


	private void openInteractionMenu() {
		final List<Plugin> plugins = InstalledPlugins.all(this);
		final LayoutInflater inflater = getLayoutInflater();
		ListView listView = (ListView) inflater.inflate(R.layout.plugins_list, null);

		listView.setAdapter(new ArrayAdapter<Plugin>(ConversationActivity.this, R.layout.plugins_list_item, plugins) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView ret = (convertView != null)
					? (TextView)convertView
					: (TextView)inflater.inflate(R.layout.plugins_list_item, null);

				Plugin plugin = plugins.get(position);
				ret.setText(plugin.caption);

				plugin.icon.setBounds(0, 0, 84, 84);    // TODO Not a good solution
				ret.setCompoundDrawables(plugin.icon, null, null, null);

				return ret;
			}
		});

		final AlertDialog dialog = new AlertDialog.Builder(this)
			.setTitle("Apps")
			.create();

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			dialog.hide();
			PluginActivities.start(ConversationActivity.this, plugins.get(position), conversation);
		}});

		dialog.setView(listView, 0, 0, 0, 0);
		dialog.show();

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
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(messageInput.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}


	private boolean isHardwareKeyboardAvailable() {
		return getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
	}

	@Override
	protected void onPause() {
		super.onPause();
		subscription.unsubscribe();
		sneer().conversations().notificationsStopIgnoring();
	}


	@Override
	protected void onResume() {
		super.onResume();
		hideKeyboard();
		sneer().conversations().notificationsStartIgnoring(conversation);
		subscription = subscribeToMessages();
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


	private Message lastMessageReceived(List<Message> ms) {
		for (int i = ms.size() - 1; i >= 0; --i) {
			Message message = ms.get(i);
			if (!message.isOwn())
				return message;
		}
		return null;
	}

}
