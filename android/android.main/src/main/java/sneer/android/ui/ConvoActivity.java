package sneer.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import sneer.android.ipc.PluginActivities;
import sneer.android.ipc.Plugins.AppType;
import sneer.convos.ChatMessage;
import sneer.convos.Convo;
import sneer.convos.Convos;
import sneer.convos.SessionSummary;
import sneer.main.R;

import static android.text.TextUtils.isEmpty;
import static sneer.android.SneerAndroidContainer.component;
import static sneer.android.SneerAndroidFlux.dispatch;
import static sneer.android.ipc.Plugins.appType;

public class ConvoActivity extends SneerActionBarActivity implements StartPluginDialogFragment.SingleConvoProvider {

	private long convoId;

	private Observable<Convo> convoObservable;
	private Subscription convoSubscription;
	private Convo currentConvo;

	private ChatAdapter chatAdapter;
	private SessionListAdapter sessionsAdapter;

	private ActionBar actionBar;
	private ViewPager viewPager;
	private ViewPagerAdapter viewPagerAdapter;

	private TextView waiting;

	private View chatView;
	private ImageButton messageButton;
	private EditText messageInput;

	private View sessionsView;


	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setContentView(R.layout.activity_convo);

		convoId = getIntent().getLongExtra("id", -1);
		convoObservable = component(Convos.class).getById(convoId);

		setupToolbar();
		setupViewPager();

		setupChatTab();
		setupSessionsTab();

		setupTabLayout();
	}


	private void setupTabLayout() {
		TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
		tabLayout.setupWithViewPager(viewPager);
	}


	private void refresh() {
		actionBar.setTitle(currentConvo.nickname);

		refreshInvitePendingMessage();
		refreshChatMessages();
		ChatMessage last = lastMessageReceived(currentConvo.messages);
		if (last != null) dispatch(currentConvo.setRead(last));

		refreshSessions();
	}


	private void refreshInvitePendingMessage() {
		boolean pending = currentConvo.inviteCodePending != null;
		final View messageList = chatView.findViewById(R.id.messageList);
		final View messageSender = chatView.findViewById(R.id.messageSender);
		messageButton.setEnabled(!pending);
		if (pending) {
			String waitingMessage = ConvoActivity.this.getResources().getString(R.string.conversation_activity_waiting);
			waiting.setText(Html.fromHtml(String.format(waitingMessage, currentConvo.nickname)));
			waiting.setMovementMethod(new LinkMovementMethod() {
				@Override
				public boolean onTouchEvent(@NonNull TextView widget, @NonNull Spannable buffer, @NonNull MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP)
						InviteSender.send(ConvoActivity.this, convoId);
					return true;
				}
			});
			messageList.setVisibility(View.GONE);
			messageSender.setVisibility(View.GONE);
			waiting.setVisibility(View.VISIBLE);
		} else {
			waiting.setVisibility(View.GONE);
			messageList.setVisibility(View.VISIBLE);
			messageSender.setVisibility(View.VISIBLE);
		}
	}


	private void refreshChatMessages() {
		chatAdapter.update(currentConvo.nickname, currentConvo.messages);
	}


	private void refreshSessions() {
		sessionsAdapter.update(currentConvo.sessionSummaries);
	}


	private void setupToolbar() {
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);     // Attaching the layout to the toolbar object
		setSupportActionBar(toolbar);                               // Setting toolbar as the ActionBar with setSupportActionBar() call
		actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowTitleEnabled(true);
	}


	private void setupViewPager() {
		viewPager = (ViewPager) findViewById(R.id.viewPager);
		viewPagerAdapter = new ViewPagerAdapter();
		viewPager.setAdapter(viewPagerAdapter);
	}


	private void setupChatTab() {
		chatView = getLayoutInflater().inflate(R.layout.fragment_convo_chat, viewPager, false);
		waiting = (TextView) chatView.findViewById(R.id.waitingMessage);
		waiting.setVisibility(View.GONE);
		setupChatMessagesList();
		setupChatMessageFields();
		viewPager.addView(chatView);
		viewPagerAdapter.addPage(chatView, "CHAT");
	}



	private void setupChatMessagesList() {
		chatAdapter = new ChatAdapter(this, getLayoutInflater());
		((ListView) chatView.findViewById(R.id.messageList)).setAdapter(chatAdapter);
	}


	private void setupChatMessageFields() {
		messageInput = (EditText) chatView.findViewById(R.id.editText);
		messageInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				messageButton.setImageResource(messageInput.getText().toString().trim().isEmpty()
						? R.drawable.ic_action_new
						: R.drawable.ic_action_send);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		messageInput.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (!isHardwareKeyboardAvailable()) return false;
				if (!(event.getAction() == KeyEvent.ACTION_DOWN)) return false;
				if (!(keyCode == KeyEvent.KEYCODE_ENTER)) return false;
				sendMessageClicked();
				return true;
			}
		});

		messageButton = (ImageButton) chatView.findViewById(R.id.actionButton);

		messageButton.setImageResource(R.drawable.ic_action_new);
		messageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMessageClicked();
			}
		});
	}


	private void sendMessageClicked() {
		if (currentConvo == null) return;

		String text = messageInput.getText().toString().trim();

		if (isEmpty(text)) {
			appType = AppType.TEXT;
			openInteractionMenu();
		} else {
			dispatch(currentConvo.sendMessage(text));
			messageInput.setText("");
		}
	}


	private void setupSessionsTab() {
		sessionsView = getLayoutInflater().inflate(R.layout.fragment_convo_sessions, viewPager, false);
		setupSessionsList();
		viewPager.addView(sessionsView);
		viewPagerAdapter.addPage(sessionsView, "SESSIONS");
	}

	private void setupSessionsList() {
		sessionsAdapter = new SessionListAdapter(this);
		ListView sessions = (ListView) sessionsView.findViewById(R.id.sessionList);
		sessions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SessionSummary summary = sessionsAdapter.getItem(position);
                System.out.println("=============== " + summary);
                PluginActivities.open(ConvoActivity.this, summary, convoId);
			}
		});
		sessions.setAdapter(sessionsAdapter);
	}


	private void openInteractionMenu() {
		StartPluginDialogFragment startPluginDialog = new StartPluginDialogFragment();
		startPluginDialog.show(getFragmentManager(), "StartPluginDialogFrament");
	}


	private void hideKeyboard() {
		if (messageInput == null) return;

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(messageInput.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}


	private boolean isHardwareKeyboardAvailable() {
		return getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
	}


	@Override
	protected void onPause() {
		unsubscribeFromConvo();
		super.onPause();
	}


	@Override
	protected void onResume() {
		super.onResume();
		hideKeyboard();
		subscribeToConvo();
	}


	private void subscribeToConvo() {
		convoSubscription = ui(convoObservable).subscribe(new Action1<Convo>() {
			@Override
			public void call(Convo convo) {
				currentConvo = convo;
				refresh();
			}
		});
	}


	private void unsubscribeFromConvo() {
		if (convoSubscription != null)
			convoSubscription.unsubscribe();
	}


	private ChatMessage lastMessageReceived(List<ChatMessage> msgs) {
		for (int i = msgs.size() - 1; i >= 0; --i) {
			ChatMessage message = msgs.get(i);
			if (!message.isOwn)
				return message;
		}
		return null;
	}


	static void open(Context context, long id) {
		Intent intent = new Intent();
		intent.setClass(context, ConvoActivity.class);
		intent.putExtra("id", id);
		context.startActivity(intent);
	}

	@Override
	public Convo getConvo() {
		return currentConvo;
	}

	public void onBtnOpenInteractionMenuClicked(View view) {
		appType = AppType.SESSION;
		openInteractionMenu();
	}


	class ViewPagerAdapter extends PagerAdapter {
		private final List<View> pageList = new ArrayList<>();
		private final List<String> pageTitleList = new ArrayList<>();

		@Override
		public Object instantiateItem(ViewGroup collection, int position) {

			System.out.println(">>>>>>>  INSTANTIATE:" + position + "; coll: " + collection);


			return pageList.get(position);
		}

		@Override
		public int getCount() {
			return pageList.size();
		}

		public void addPage(View page, String title) {
			pageList.add(page);
			pageTitleList.add(title);
			notifyDataSetChanged();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return pageTitleList.get(position);
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == ((View) arg1);
		}

	}

}
