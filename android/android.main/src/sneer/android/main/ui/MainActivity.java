package sneer.android.main.ui;

import static sneer.android.main.SneerApp.*;
import static sneer.android.ui.SneerActivity.*;

import java.io.*;
import java.util.*;

import rx.Observable;
import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.android.main.core.*;
import sneer.commons.*;
import sneer.commons.exceptions.*;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import clojure.java.api.*;

public class MainActivity extends Activity {
	
	private MainAdapter adapter;
	private ListView listView;
	private Conversation conversation;
	private EmbeddedOptions embeddedOptions;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!SneerApp.checkOnCreate(this)) return;
		
		setContentView(R.layout.activity_main);

		startProfileActivityIfFirstTime();
//		startCore();
		SneerSqliteDatabase.selfTest();
		
		makeConversationList();
	}


	/**
	 * Used when this activity is launched by another application with options
	 * to modify its behavior
	 * 
	 * @author fabio
	 * 
	 */
	public static class EmbeddedOptions implements Serializable {
		private static final long serialVersionUID = 1L;
		public String conversationAction;
		public String type;
		public String title;
		public String conversationLabel;
		public boolean disableMenus;

		public EmbeddedOptions(Intent intent) {
			if (intent == null || intent.getExtras() == null)
				return;

			conversationAction = intent.getExtras().getString(SneerAndroid.NEW_CONVERSATION_ACTION);
			conversationLabel = intent.getExtras().getString(SneerAndroid.NEW_CONVERSATION_LABEL);
			type = intent.getExtras().getString(SneerAndroid.TYPE);
			title = intent.getExtras().getString(SneerAndroid.TITLE);
			disableMenus = intent.getExtras().getBoolean(SneerAndroid.DISABLE_MENUS, false);
		}

		public boolean wasEmbedded() {
			return conversationAction != null;
		}
	}
	

	private void makeConversationList() {
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setHomeButtonEnabled(true);

		embeddedOptions = new EmbeddedOptions(getIntent());

		if (embeddedOptions.title != null) {
			actionBar.setTitle(embeddedOptions.title);
		}

		sneer().profileFor(sneer().self()).ownName()
				.subscribe(new Action1<String>() { @Override public void call(String label) {
					actionBar.setTitle(label);
				}});

		sneer().profileFor(sneer().self()).selfie()
				.observeOn(AndroidSchedulers.mainThread()).cast(byte[].class)
				.subscribe(new Action1<byte[]>() { @Override public void call(byte[] selfie) {
					actionBar.setIcon((Drawable) new BitmapDrawable(
							getResources(), BitmapFactory.decodeByteArray(
									selfie, 0, selfie.length)));
				}});

		listView = (ListView) findViewById(R.id.listView);
		adapter = new MainAdapter(this,
				R.layout.list_item_main,
				new Func1<Party, Observable<String>>() { @Override public Observable<String> call(Party party) {
					return party.name();
				}},
				new Func1<Party, Observable<byte[]>>() { @Override public Observable<byte[]> call(Party party) {
					return sneer().profileFor(party).selfie();
				}});

		listView.setAdapter(adapter);

		registerForContextMenu(listView);

		listView.setOnItemClickListener(new OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
			Conversation conversation = adapter.getItem(position);
			onContactClicked(conversation);
		}});

		sneer().conversationsContaining(embeddedOptions.type)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<Collection<Conversation>>() { @Override public void call(Collection<Conversation> conversations) {
					adapter.clear();
					adapter.addAll(conversations);
					adapter.notifyDataSetChanged();
				}});
	}
	

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		conversation = adapter.getItem(info.position);
		plugHeaderTitle(menu, conversation.party().name());
		getMenuInflater().inflate(R.menu.long_click, menu);
	}
	

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.chat_contact:
			toast(conversation.toString()); // Do something useful with conversation
											// here.
			break;
		case R.id.edit_contact:
			conversation.toString(); // Do something useful with conversation
									// here.
			break;
		case R.id.remove_contact:
			conversation.toString(); // Do something useful with conversation
									// here.
			break;
		}
		return true;
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (embeddedOptions.disableMenus) {
			return false;
		}
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
	
	
	private void navigateTo(Class<?> class1) {
		startActivity(new Intent().setClass(this, class1));
	}
	

	protected void onContactClicked(Conversation conversation) {
		Intent intent = new Intent();
		intent.setClass(this, ConversationActivity.class);
		intent.putExtra("embeddedOptions", embeddedOptions);
		intent.putExtra("partyPuk", conversation.party().publicKey().current());
		startActivity(intent);
	}

	
	@Override
	protected void onDestroy() {
		adapter.dispose();
		super.onDestroy();
	}

	
	private void startCore() {
		Object result;
		try {
			Clojure.var("clojure.core/require").invoke(Clojure.read("sneer.networking.client"));
			result = Clojure.var("sneer.networking.client/start").invoke(sneer().self().publicKey().current());
		} catch (Exception e) {
			Log.w(MainActivity.class.getSimpleName(), e);
			result = Exceptions.asNiceMessage(e);
		}
		SystemReport.updateReport("networking.client", result);
	}

	
	private void startProfileActivityIfFirstTime() {
		ownName().subscribe(new Action1<String>() { @Override public void call(String name) {
			if (name.isEmpty())
				startActivity(new Intent(MainActivity.this, ProfileActivity.class));
		}});
	}


	private Observable<String> ownName() {
		return sneer().profileFor(sneer().self()).ownName();
	}

	
	private void toast(String message) {
		Log.d(MainActivity.class.getSimpleName(), "toast: " + message);
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}
	
	
	public static void log(String s) {
		Log.d(MainActivity.class.getSimpleName(), s);
	}
	
}
