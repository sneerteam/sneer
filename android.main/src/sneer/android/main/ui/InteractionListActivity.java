package sneer.android.main.ui;

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
	private InteractionsAdapter adapter;
	private Cloud cloud;
	private ListView listView;
	private Interaction interaction;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_interaction_list);

		if (getIntent() != null && getIntent().getExtras() != null
				) {
			String title;
			
			if ((title = getIntent().getExtras().getString(TITLE)) != null) {				
				setTitle(title);				
			}
		
		}

		final Sneer sneer;
		try {
			sneer = SneerSingleton.SNEER_ADMIN.initialize(Keys.createPrivateKey());
		} catch (FriendlyException e1) {
		 	toast(e1.getMessage());
		 	this.finish();
			return;
		}

		listView = (ListView)findViewById(R.id.listView);
		adapter = new InteractionsAdapter(this, R.layout.list_item_interaction, new Func1<Party, Observable<String>>() {  @Override public Observable<String> call(Party party) {
			return sneer.labelFor(party).observable();
		}});
		listView.setAdapter(adapter);

		registerForContextMenu(listView);

		listView.setOnItemClickListener(new OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
			Interaction interaction = adapter.getItem(position);
			onContactClicked(interaction);
		}});

		sneer.interactions().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Collection<Interaction>>() { @Override public void call(Collection<Interaction> interactions) {
			for (Interaction interaction : interactions)
				adapter.add(interaction);
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
			sendMyPublicKey();
			break;
		case R.id.action_copy_pk:
			copyMyPublicKey();
			break;
		}

		return true;
	}

	
	private void showContactAdd() {
//		new ContactAddHelper(this, new ContactAddHelper.AddListener() {
//			@Override
//			public void add(final OldContact interaction) {
//				CloudPath contactPath = cloud.path("contacts",
//						interaction.getPublicKey());
//				contactPath.append("nickname").pub(interaction.getNickname());
//			}
//		});
	}

	
	public static void log(String s) {
		Log.d(InteractionListActivity.class.getSimpleName(), s);
	}

	
	private void showProfile() {
		startActivity(new Intent(this, ProfileActivity.class));
	}

	
	private void sendMyPublicKey() {
		cloud.ownPublicKey().subscribe(new Action1<byte[]>() {

			@Override
			public void call(byte[] publicKey) {
//				String shareBody = PublicKey.bytesToHex(publicKey);
//				log("ownPublicKey: " + shareBody);
//				Intent sharingIntent = new Intent(Intent.ACTION_SEND);
//				sharingIntent.setType("text/plain");
//				// sharingIntent.putExtra(Intent.EXTRA_SUBJECT,
//				// "My Sneer public key");
//				sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
//				startActivity(sharingIntent);
			}
		});
	}

	
	private void copyMyPublicKey() {
		cloud.ownPublicKey().subscribe(new Action1<byte[]>() {
			@Override
			public void call(byte[] publicKey) {
//				ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
//				ClipData clip = ClipData.newPlainText("Public Key",
//						PublicKey.bytesToHex(publicKey));
//				clipboard.setPrimaryClip(clip);
			}
		});
	}

	
	protected void onContactClicked(Interaction interaction) {
//		Bundle extras = getIntent().getExtras();
//		Intent intent = new Intent(extras.getString("Action"));
//		intent.putExtra("partyPuk", interaction.party().publicKey().toBlockingObservable().first());
//		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		startActivity(intent);
	}

}
