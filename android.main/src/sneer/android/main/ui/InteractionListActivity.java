package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.*;

import java.util.*;

import rx.Observable;
import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.android.main.R;
import sneer.commons.exceptions.*;
import sneer.impl.keys.*;
import sneer.snapi.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class InteractionListActivity extends Activity {

	private static final String DISABLE_MENUS = "disable-menus";
	private static final String TITLE = "title";
	private InteractionListAdapter adapter;
	private Cloud cloud;
	private ListView listView;
	private Interaction interaction;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_interaction_list);

		if (getIntent() != null && getIntent().getExtras() != null) {
			String title;
			
			if ((title = getIntent().getExtras().getString(TITLE)) != null) {				
				setTitle(title);
			}		
		}

		listView = (ListView)findViewById(R.id.listView);
		adapter = new InteractionListAdapter(this, R.layout.list_item_interaction, new Func1<Party, Observable<String>>() {  @Override public Observable<String> call(Party party) {
			return sneer().labelFor(party).observable();
		}});
		listView.setAdapter(adapter);

		registerForContextMenu(listView);

		listView.setOnItemClickListener(new OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
			Interaction interaction = adapter.getItem(position);
			onContactClicked(interaction);
		}});

		sneer().interactions().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Collection<Interaction>>() { @Override public void call(Collection<Interaction> interactions) {
			adapter.clear();
			adapter.addAll(interactions);
			adapter.notifyDataSetChanged();
		}});
	}

	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		interaction = adapter.getItem(info.position);
		menu.setHeaderTitle(interaction.party().name().toBlockingObservable().first());
		getMenuInflater().inflate(R.menu.long_click, menu);
	}

	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.chat_contact:
			toast(interaction.toString()); // Do something useful with interaction here.
			break;
		case R.id.edit_contact:
			interaction.toString(); // Do something useful with interaction here.
			break;
		case R.id.remove_contact:
			interaction.toString(); // Do something useful with interaction here.
			break;
		}

		return true;
	}

	
	@Override
	protected void onDestroy() {
		if(cloud!=null)
			cloud.dispose();
		super.onDestroy();
	}

	
	void toast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (getIntent() != null && getIntent().getExtras() != null
				&& getIntent().getExtras().getBoolean(DISABLE_MENUS)) {
			return false;
		}
		getMenuInflater().inflate(R.menu.contacts, menu);
		return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_add_contact:
			showContactAdd();
			break;
		case R.id.action_profile:
			showProfile();
			break;
		case R.id.action_send_pk:
			sendYoursPublicKey();
			break;
		case R.id.action_copy_pk:
			copyYoursPublicKey();
			break;
		}

		return true;
	}

	
	private void showContactAdd() {
		View addContactView = View.inflate(this, R.layout.activity_contact_add, null);
		final EditText publicKeyEdit = (EditText) addContactView.findViewById(R.id.public_key);
		final EditText nicknameEdit = (EditText) addContactView.findViewById(R.id.nickname);
		AlertDialog alertDialog = new AlertDialog.Builder(this)
			.setView(addContactView)
			.setTitle(R.string.action_add_contact)
			.setNegativeButton("Cancel", null)
			.setPositiveButton("Add", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) {
				PublicKey puk = Keys.createPublicKey(publicKeyEdit.getText().toString().getBytes());
				String nickname = nicknameEdit.getText().toString();
				Party party = sneer().produceParty((PublicKey)puk);
				try {
					sneer().setContact(nickname, party);
				} catch (FriendlyException e) {
					toast(e.getMessage());
				}
			}})
			.create();
		alertDialog.show();
	}

	
	public static void log(String s) {
		Log.d(InteractionListActivity.class.getSimpleName(), s);
	}

	
	private void showProfile() {
		startActivity(new Intent(this, ProfileActivity.class));
	}

	
	private void sendYoursPublicKey() {
		log("ownPublicKey: " + sneer().self().publicKey().mostRecent());
		Intent sharingIntent = new Intent(Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "My Sneer public key");
		sharingIntent.putExtra(Intent.EXTRA_TEXT, sneer().self().publicKey().mostRecent().toString());
		startActivity(sharingIntent);
	}

	
	private void copyYoursPublicKey() {
		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("Public Key", sneer().self().publicKey().mostRecent().toString());
		clipboard.setPrimaryClip(clip);
	}

	
	protected void onContactClicked(Interaction interaction) {
		Intent intent = new Intent(this, InteractionActivity.class);
		intent.putExtra("partyPuk", interaction.party().publicKey().mostRecent());
		startActivity(intent);
	}

}
