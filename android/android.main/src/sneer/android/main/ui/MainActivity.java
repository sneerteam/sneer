package sneer.android.main.ui;

import static sneer.android.main.SneerApp.admin;
import static sneer.android.main.SneerApp.sneer;
import static sneer.android.main.SneerApp.sneerAndroid;
import static sneer.android.main.SneerAndroid.*;

import java.util.*;

import rx.Observable;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.android.ui.*;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends SneerActivity {
	
	private MainAdapter adapter;
	private ListView conversations;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!sneerAndroid(this).checkOnCreate(this)) return;
		
		setContentView(R.layout.activity_main);

		startProfileActivityIfFirstTime();
		
		makeConversationList();
	}


	private void makeConversationList() {
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setHomeButtonEnabled(true);

		Profile ownProfile = sneer(this).profileFor(sneer(this).self());
		
		plugActionBarTitle(actionBar, ownProfile.ownName());
		plugActionBarIcon(actionBar, ownProfile.selfie());

		conversations = (ListView) findViewById(R.id.conversationList);
		adapter = new MainAdapter(this);
		conversations.setAdapter(adapter);

		conversations.setOnItemClickListener(new OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
			Conversation conversation = adapter.getItem(position);
			onClicked(conversation);
		}});
		
		deferUI(sneer(this).conversations())
			.subscribe(new Action1<Collection<Conversation>>() { @Override public void call(Collection<Conversation> conversations) {
				adapter.clear();
				adapter.addAll(conversations);
				adapter.notifyDataSetChanged();
			}});
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
	
	
	protected void onClicked(Conversation conversation) {
		Intent intent = new Intent();
		intent.setClass(this, ConversationActivity.class);
		intent.putExtra("partyPuk", conversation.party().publicKey().current());
		startActivity(intent);
	}

	
	@Override
	protected void onDestroy() {
		adapter.dispose();
		super.onDestroy();
	}

	
	private void startProfileActivityIfFirstTime() {
		ownName().subscribe(new Action1<String>() { @Override public void call(String name) {
			if (name.isEmpty())
				startActivity(new Intent(MainActivity.this, ProfileActivity.class));
		}});
	}


	private Observable<String> ownName() {
		return sneer(this).profileFor(sneer(this).self()).ownName();
	}

	
	public static void log(String s) {
		Log.d(MainActivity.class.getSimpleName(), s);
	}
	
}
