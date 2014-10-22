package sneer.android.main.ui;

import static sneer.android.main.ui.SneerAndroidProvider.sneer;
import static sneer.android.main.ui.SneerAndroidProvider.sneerAndroid;

import java.util.Collection;

import rx.functions.Action1;
import sneer.Conversation;
import sneer.Party;
import sneer.Profile;
import sneer.android.main.R;
import sneer.android.main.utils.Puk;
import sneer.android.ui.SneerActivity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends SneerActivity {
	
	static final boolean SIMULATOR = false;

	private MainAdapter adapter;
	private ListView conversations;

	private Party self = sneer().self();
	private Profile ownProfile = sneer().profileFor(self);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!sneerAndroid().checkOnCreate(this)) return;
		if (!SIMULATOR) startProfileActivityIfFirstTime();
		
		makeConversationList();
	}


	private void makeConversationList() {
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setHomeButtonEnabled(true);

		
		plugActionBarTitle(actionBar, ownProfile.ownName());
		plugActionBarIcon(actionBar, ownProfile.selfie());

		conversations = (ListView) findViewById(R.id.conversationList);
		adapter = new MainAdapter(this);
		conversations.setAdapter(adapter);

		conversations.setOnItemClickListener(new OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
			Conversation conversation = adapter.getItem(position);
			onClicked(conversation);
		}});
		
		deferUI(sneer().conversations()).subscribe(new Action1<Collection<Conversation>>() { @Override public void call(Collection<Conversation> conversations) {
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
		case R.id.action_add_contact:
			shareDialog();
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
	
	
	private void shareDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage("To add contacts, send them your public key and they must send you theirs.")
			.setIcon(android.R.drawable.ic_dialog_info)
			.setPositiveButton("Send Public Key", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int which) {
				Puk.sendYourPublicKey(MainActivity.this, self, true, null);
			}})
			.show();
	}

	
	protected void onClicked(Conversation conversation) {
		Intent intent = new Intent();
		intent.setClass(this, ConversationActivity.class);
		intent.putExtra("partyPuk", conversation.party().publicKey().current());
		startActivity(intent);
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
		if (isOwnNameLocallyAvailable()) return;
		finish();
		toast("First and last name must be filled in");
	}
	
	
	private void startProfileActivityIfFirstTime() {
		if (!isOwnNameLocallyAvailable())
			navigateTo(ProfileActivity.class);
	}
	
	
	private boolean isOwnNameLocallyAvailable() {
		return ownProfile.isOwnNameLocallyAvailable();
	}
	
}
