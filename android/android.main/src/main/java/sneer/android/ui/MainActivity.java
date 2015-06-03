package sneer.android.ui;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.util.Collection;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import sneer.Conversation;
import sneer.Party;
import sneer.Profile;
import sneer.Sneer;
import sneer.android.SneerApp;
import sneer.main.R;
import sneer.rx.ObservedSubject;

import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.EXTRA_SUBJECT;
import static android.content.Intent.EXTRA_TEXT;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static sneer.android.SneerAndroidContainer.component;
import static sneer.android.SneerAndroidSingleton.sneerAndroid;

public class MainActivity extends SneerActivity {

	private MainAdapter adapter;

	private final Party self = initSelf();
	private final Profile ownProfile = sneer().profileFor(self);

    private String subjectToSend;
    private String textToSend;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!sneerAndroid().checkOnCreate(this)) return;

		handleFirstTime();

		setContentView(R.layout.activity_main);
		makeConversationList();

		handleSend(getIntent());

//		final ActionBar actionBar = getActionBar();
//		onMainThread(subject.observable()).subscribe(new Action1<String>() { @Override public void call(String value) {
//			actionBar.setTitle(value);
//			synchronized (monitor) {
//				monitor.notify();
//			}
//		}});
	}




	private boolean benchmarkRunning = true;
	private final Object monitor = new Object();
	private int counter = 0;
	ObservedSubject<String> subject = ObservedSubject.create("");
	private void startBenchmarkThread() {
		benchmarkRunning = true;

		new Thread() { @Override public void run() {
			while (benchmarkRunning) {
				synchronized (monitor) {
					subject.onNext(counter++ + " Thread:" + Thread.currentThread().hashCode());
					try { monitor.wait(); } catch (InterruptedException e) { throw new IllegalStateException(e); }
				}
			}
		}}.start();
	}








	private void handleSend(Intent intent) {
		if (intent == null) return;
		if (intent.getAction() == null) return;
		if (!intent.getAction().equals(ACTION_SEND )) return;
		if (!intent.getType()  .equals("text/plain")) return;
		subjectToSend = intent.getStringExtra(EXTRA_SUBJECT);
		textToSend = intent.getStringExtra(EXTRA_TEXT);
	}


	private void makeConversationList() {
		final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            plugActionBarTitle(actionBar, ownProfile.ownName());
            plugActionBarIcon(actionBar, ownProfile.selfie());
        }

		final RelativeLayout addContactTutorial = (RelativeLayout) findViewById(R.id.add_contact_tutorial);

		adapter = new MainAdapter(this);
		deferUI(sneer().conversations().all()).subscribe(new Action1<Collection<Conversation>>() { @Override public void call(Collection<Conversation> conversations) {
			adapter.clear();
			adapter.addAll(conversations);
			adapter.notifyDataSetChanged();

			addContactTutorial.setVisibility(adapter.getCount() == 0 ? VISIBLE : GONE);
		}});

		ListView conversationList = (ListView) findViewById(R.id.conversationList);
		conversationList.setAdapter(adapter);
		conversationList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
				Conversation conversation = adapter.getItem(position);
				onClicked(conversation);
			}
		});
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			navigateTo(ProfileActivity.class);
			break;
		case R.id.action_add_contact:
//			startBenchmarkThread();  // To use the benchmark uncomment this line and comment the ones below in this case.
			navigateTo(AddContactActivity.class);
			break;
		case R.id.action_search_for_apps:
			Intent viewIntent =
	          new Intent("android.intent.action.VIEW", Uri.parse("https://play.google.com/store/search?q=SneerApp"));
	          startActivity(viewIntent);
			break;
		case R.id.action_advanced:
			navigateTo(SystemReportActivity.class);
			break;
		}

		return true;
	}


	private void onClicked(Conversation conversation) {
		sendMessageIfPresent(conversation);
		Intent intent = new Intent();
		intent.setClass(this, ConversationActivityOld.class);
		intent.putExtra("nick", conversation.contact().nickname().current());
		startActivity(intent);
	}

	private void sendMessageIfPresent(final Conversation conversation) {
		if (subjectToSend == null && textToSend == null) return;
		String separator = (subjectToSend != null && textToSend != null)
				? "\n\n" : "";
		final String message = ensure(subjectToSend) + separator + ensure(textToSend);
		subjectToSend = null;
		textToSend = null;
		conversation.canSendMessages().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Boolean>() { @Override public void call(Boolean canSend) {
			if (canSend)
				conversation.sendMessage(message);
			else
				toastLong("This conversation cannot send messages yet");
		}});
	}

	private String ensure(String s) {
		return s == null ? "" : s;
	}


	@Override
	protected void onDestroy() {
		if (adapter != null)
			adapter.dispose();
		super.onDestroy();
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		adapter.notifyDataSetChanged();
		checkHasOwnName();
	}


	private void checkHasOwnName() {
		if (isOwnNameLocallyAvailable()) return;
		finish();
		toast("Name must be filled in");
	}


	@Override protected void onPause()  { super.onPause();  Notifier.resume(); benchmarkRunning = false; }
	@Override protected void onResume() { super.onResume(); Notifier.pause(); }

	private void handleFirstTime() {
		if (isOwnNameLocallyAvailable()) return;
		((SneerApp)getApplication()).checkPlayServices(this);
		navigateTo(ProfileActivity.class);
	}

	private Party initSelf() {
		try {
			return sneer().self();
		} catch (NullPointerException npe) {
			throw new RuntimeException("MainActivity needs the Core. To work without the Core, launch a different Activity.");
		}
	}

	private boolean isOwnNameLocallyAvailable() {
		return ownProfile.isOwnNameLocallyAvailable();
	}


	private static Sneer sneer() {
		return component(Sneer.class);
	}

}
