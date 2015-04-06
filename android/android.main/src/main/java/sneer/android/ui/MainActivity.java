package sneer.android.ui;

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
import android.widget.RelativeLayout;

import java.util.Collection;

import me.sneer.R;
import rx.functions.Action1;
import sneer.Conversation;
import sneer.Party;
import sneer.Profile;
import sneer.android.Notifier;
import sneer.android.SneerApp;
import sneer.android.ui.adapters.MainAdapter;

import static sneer.android.SneerAndroidSingleton.sneer;
import static sneer.android.SneerAndroidSingleton.sneerAndroid;
import static sneer.android.utils.Puk.shareOwnPublicKey;

public class MainActivity extends SneerActivity {

	private MainAdapter adapter;
	private ListView conversationList;

	private final Party self = sneer().self();
	private final Profile ownProfile = sneer().profileFor(self);


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!sneerAndroid().checkOnCreate(this)) return;

		startProfileActivityIfFirstTime();

		makeConversationList();

        ((SneerApp)getApplication()).checkPlayServices(this);
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

			if (ContactActivity.USE_INVITES)
				if (adapter.getCount() == 0)
					addContactTutorial.setVisibility(View.VISIBLE);
				else
					addContactTutorial.setVisibility(View.GONE);
		}});

		conversationList = (ListView) findViewById(R.id.conversationList);
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
			if (ContactActivity.USE_INVITES)
				navigateTo(AddContactActivity.class);
			else
				openSharePukDialog();
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


	private void openSharePukDialog() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage("To add contacts, send them your public key and they must send you theirs.")
				.setIcon(android.R.drawable.ic_dialog_info)
				.setPositiveButton("Send Public Key", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialog, int which) {
					shareOwnPublicKey(MainActivity.this, sneer().self(), null, "");
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
		adapter.notifyDataSetChanged();
		if (isOwnNameLocallyAvailable()) return;
		finish();
		toast("First and last name must be filled in");
	}

	@Override protected void onPause()  { super.onPause();  Notifier.resume(); }
	@Override protected void onResume() { super.onResume(); Notifier.pause(); }

	private void startProfileActivityIfFirstTime() {
		if (!isOwnNameLocallyAvailable())
			navigateTo(ProfileActivity.class);
	}


	private boolean isOwnNameLocallyAvailable() {
		return ownProfile.isOwnNameLocallyAvailable();
	}

}
