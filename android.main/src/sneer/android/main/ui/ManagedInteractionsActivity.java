package sneer.android.main.ui;

import static sneer.android.ui.UIUtils.*;
import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.Contact;
import sneer.android.main.R;
import sneer.keys.*;
import sneer.snapi.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class ManagedInteractionsActivity extends Activity {

	private static final String DISABLE_MENUS = "disable-menus";
	private static final String TITLE = "title";
	private ContactsAdapter adapter;
	private Cloud cloud;
	private ListView listView;
	private Contact contact;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (getIntent() != null && getIntent().getExtras() != null
				) {
			String title;
			
			if ((title = getIntent().getExtras().getString(TITLE)) != null) {				
				setTitle(title);
				
			}
		
		}
		
		

		listView = (ListView) findViewById(R.id.listView);
		adapter = new ContactsAdapter(this, R.layout.list_item_contact);
		listView.setAdapter(adapter);

		registerForContextMenu(listView);

		listView.setOnItemClickListener(new OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
			Contact contact = adapter.getItem(position);
			onContactClicked(contact);
		}});

		SneerSingleton.SNEER.contacts().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Contact>() {  @Override public void call(Contact newContact) {
			adapter.add(newContact);
		}});
	}

	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		contact = adapter.getItem(info.position);
		subscribeMenuHeader(menu, contact.nickname());
		getMenuInflater().inflate(R.menu.long_click, menu);
	}

	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.chat_contact:
			toast(contact.toString()); // Do something useful with contact here.
			break;
		case R.id.edit_contact:
			contact.toString(); // Do something useful with contact here.
			break;
		case R.id.remove_contact:
			contact.toString(); // Do something useful with contact here.
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
			sendMyPublicKey();
			break;
		case R.id.action_copy_pk:
			copyMyPublicKey();
			break;
		}

		return true;
	}

	
	private void showContactAdd() {
		new ContactAddHelper(this, new ContactAddHelper.AddListener() {
			@Override
			public void add(final OldContact contact) {
				CloudPath contactPath = cloud.path("contacts",
						contact.getPublicKey());
				contactPath.append("nickname").pub(contact.getNickname());
			}
		});
	}

	
	public static void log(String s) {
		Log.d(ManagedInteractionsActivity.class.getSimpleName(), s);
	}

	
	private void showProfile() {
		startActivity(new Intent(this, ProfileActivity.class));
	}

	
	private void sendMyPublicKey() {
		cloud.ownPublicKey().subscribe(new Action1<byte[]>() {

			@Override
			public void call(byte[] publicKey) {
				String shareBody = PublicKey.bytesToHex(publicKey);
				log("ownPublicKey: " + shareBody);
				Intent sharingIntent = new Intent(Intent.ACTION_SEND);
				sharingIntent.setType("text/plain");
				// sharingIntent.putExtra(Intent.EXTRA_SUBJECT,
				// "My Sneer public key");
				sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
				startActivity(sharingIntent);
			}
		});
	}

	
	private void copyMyPublicKey() {
		cloud.ownPublicKey().subscribe(new Action1<byte[]>() {
			@Override
			public void call(byte[] publicKey) {
				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("Public Key",
						PublicKey.bytesToHex(publicKey));
				clipboard.setPrimaryClip(clip);

			}
		});
	}

	
	protected void onContactClicked(Contact contact) {
//		Bundle extras = getIntent().getExtras();
//		Intent intent = new Intent(extras.getString("Action"));
//		intent.putExtra("partyPuk", contact.party().publicKey().toBlockingObservable().first());
//		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		startActivity(intent);
	}

}
