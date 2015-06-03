package sneer.android.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import sneer.Contact;
import sneer.Party;
import sneer.PublicKey;
import sneer.commons.exceptions.FriendlyException;
import sneer.main.R;

import static sneer.android.SneerAndroidSingleton.admin;
import static sneer.android.SneerAndroidSingleton.sneer;
import static sneer.android.SneerAndroidSingleton.sneerAndroid;
import static sneer.android.ui.SneerActivity.plug;

public class ContactActivity extends Activity {

	static final String CURRENT_NICKNAME = "currentNickname";

	private ActionBar actionBar;

	private boolean newContact = false;
	private Contact contact;

	private EditText nicknameEdit;
	private Party party = null;
	private PublicKey partyPuk;
	private String inviteCode;
	private Intent intent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!sneerAndroid().checkOnCreate(this)) return;

		intent = getIntent();

		if (!extractPuk()) {
			toast("Contact not found");
			finish();
			return;
		}

		if (!validInviteCode()) {
			toast("Invalid invite code");
			finish();
			return;
		}

		if (partyPuk.equals(sneer().self().publicKey().current())) {
			toast("That is your own public key");
			finish();
			return;
		}

		setContentView(R.layout.activity_contact);

		actionBar = getActionBar();
		if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

		nicknameEdit = (EditText) findViewById(R.id.nickname);

		loadContact();
		plug(nicknameEdit, contact.nickname().observable());
		validationOnTextChanged(nicknameEdit);
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
				Intent upIntent = NavUtils.getParentActivityIntent(this);
				if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
					// This activity is NOT part of this app's task, so create a new task
					// when navigating up, with a synthesized back stack.
					TaskStackBuilder.create(this)
							// Add all of this activity's parents to the back stack
							.addNextIntentWithParentStack(upIntent)
							// Navigate up to the closest parent
							.startActivities();
				}
				finish();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}


	private boolean extractPuk() {
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			String[] query = intent.getData().getQuery().split("&invite=");
			try {
				partyPuk = admin().keys().createPublicKey(query[0]);
			} catch (RuntimeException e) {
				return false;
			}
		} else {
			Contact c = sneer().findByNick(intent.getStringExtra(CURRENT_NICKNAME));
			if (c == null) return false;
			Party p = c.party().current();
			if (p == null) return false;
			partyPuk = p.publicKey().current();
		}
		return true;
	}


	private boolean validInviteCode() {
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			String[] query = intent.getData().getQuery().split("&invite=");

			if (query.length > 1)
				inviteCode = query[1];

			if (TextUtils.isEmpty(inviteCode))
				return false;

			// TODO: check on database
		}
		return true;
	}


	private void loadContact() {
		party = sneer().produceParty(partyPuk);
		contact = sneer().findContact(party);
		newContact = contact == null;

		if (actionBar == null) return;
		actionBar.setTitle(newContact ? "New Contact" : "Contact");
	}


	private void validationOnTextChanged(final EditText editText) {
		editText.addTextChangedListener(new TextWatcher() {

			@Override public void afterTextChanged(Editable s) {
				nicknameEdit.setError(sneer().problemWithNewNickname(editText.getText().toString(), party));
			}

			@Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
		});
	}


	private void saveContact() {
		boolean changed = false;
		final String nickName = text(nicknameEdit);
		try {
			if (newContact) {
				sneer().produceContact(nickName, party, inviteCode);
				changed = true;
			}

			if (!nickName.equals(intent.getExtras().getString(CURRENT_NICKNAME))) {
				sneer().findContact(party).setNickname(nickName);
				changed = true;
			}
		} catch (FriendlyException e) {
			toast(e.getMessage());
			return;
		}

		if (changed)
			toast("Contact saved");
	}


	@Override
	protected void onPause() {
		super.onPause();
		saveContact();
	}


	private void toast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}


	private String text(EditText editText) {
		return editText.getText().toString().trim();
	}

}
