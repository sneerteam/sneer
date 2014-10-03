package sneer.android.main.ui;

import static sneer.android.main.ui.SneerAndroidProvider.sneer;
import static sneer.android.main.ui.SneerAndroidProvider.sneerAndroid;

import java.util.Collection;

import rx.functions.Action1;
import sneer.Conversation;
import sneer.Profile;
import sneer.android.main.R;
import sneer.android.ui.SneerActivity;
import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class MainActivity extends SneerActivity {
	
	private MainAdapter adapter;
	private ListView conversations;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!sneerAndroid().checkOnCreate(this)) return;
		
		setContentView(R.layout.activity_main);

		startProfileActivityIfFirstTime();
		
		makeConversationList();
	}


	private void makeConversationList() {
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setHomeButtonEnabled(true);

		Profile ownProfile = sneer().profileFor(sneer().self());
		
		plugActionBarTitle(actionBar, ownProfile.ownName());
		plugActionBarIcon(actionBar, ownProfile.selfie());

		conversations = (ListView) findViewById(R.id.conversationList);
		adapter = new MainAdapter(this);
		conversations.setAdapter(adapter);

		conversations.setOnItemClickListener(new OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
			Conversation conversation = adapter.getItem(position);
			onClicked(conversation);
		}});
		
		deferUI(sneer().conversations())
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
//		if (sneer().profileFor(sneer().self()).isOwnNameLocallyAvailable())
//			startActivity(new Intent(MainActivity.this, ProfileActivity.class));
	}


	public static void log(String s) {
		Log.d(MainActivity.class.getSimpleName(), s);
	}
	
}
