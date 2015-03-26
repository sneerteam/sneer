package sneer.android.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import me.sneer.R;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import sneer.Contact;
import sneer.Party;
import sneer.Profile;
import sneer.PublicKey;
import sneer.commons.exceptions.FriendlyException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static sneer.android.SneerAndroidSingleton.admin;
import static sneer.android.SneerAndroidSingleton.sneer;
import static sneer.android.SneerAndroidSingleton.sneerAndroid;
import static sneer.android.ui.SneerActivity.plug;

public class ContactActivity extends Activity {

	static final String PARTY_PUK = "partyPuk";
	static final String CURRENT_NICKNAME = "currentNickname";

	private ActionBar actionBar;

	private boolean newContact = false;
	private Profile profile;
	private ImageView selfieImage;

	private EditText nicknameEdit;
	private TextView fullNameView;
	private TextView preferredNicknameView;
	private TextView countryView;
	private TextView cityView;
	private Party party = null;
	private PublicKey partyPuk;
	private String inviteCode;
	private Intent intent;
	private Contact contact;
	private boolean isOwn;
	private Subscription ownNameSubscription;
	private Subscription preferredNicknameSubscription;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!sneerAndroid().checkOnCreate(this)) return;

		intent = getIntent();

		if (!validPuk()) {
			toast("Invalid public key");
			finish();
			return;
		}

		if (!validInviteCode()) {
			toast("Invalid invite code");
			finish();
			return;
		}

		if (partyPuk.toHex().equals(sneer().self().publicKey().current().toHex())) {
			isOwn = true;
			startActivity(new Intent().setClass(this, ProfileActivity.class));
			finish();
			return;
		}

		setContentView(R.layout.activity_contact);

		nicknameEdit = (EditText) findViewById(R.id.nickname);
		fullNameView = (TextView) findViewById(R.id.fullName);
		preferredNicknameView = (TextView) findViewById(R.id.preferredNickName);
		selfieImage = (ImageView) findViewById(R.id.selfie);
		countryView = (TextView) findViewById(R.id.country);
		cityView = (TextView) findViewById(R.id.city);

		actionBar = getActionBar();
		if (actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled(true);

		loadContact();
		plugProfile();
		hidePreferredNicknameWhenNeeded();
		validationOnTextChanged(nicknameEdit);
	}


	private boolean validPuk() {
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			String[] query = intent.getData().getQuery().split("&invite=");
			try {
				partyPuk = admin().keys().createPublicKey(query[0]);
			} catch (RuntimeException e) {
				return false;
			}
		} else {
			Bundle extras = intent.getExtras();
			partyPuk = (PublicKey) extras.getSerializable(PARTY_PUK);
		}
		return true;
	}


	private boolean validInviteCode() {
		if (Intent.ACTION_VIEW.equals(intent.getAction())) {
			String[] query = intent.getData().getQuery().split("&invite=");

			inviteCode = "";
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

		profile = sneer().profileFor(party);
		contact = sneer().findContact(party);
		newContact = contact == null;

		if (actionBar == null) return;
		actionBar.setTitle(newContact ? "New Contact" : "Contact");
	}


	private void plugProfile() {
		if (newContact) {
			ownNameSubscription = plug(nicknameEdit, profile.preferredNickname().mergeWith(profile.ownName().delay(100, MILLISECONDS)).first());
			if (nicknameEdit.getText().toString().isEmpty())
				preferredNicknameSubscription = plug(nicknameEdit, profile.preferredNickname());
		} else {
			plug(nicknameEdit, contact.nickname().observable());
		}

		plug(fullNameView, profile.ownName());
		plug(preferredNicknameView, profile.preferredNickname().map(new Func1<Object, String>() { @Override public String call(Object obj) {
			return "(" + obj.toString() + ")";
		}}));
		plug(countryView, profile.country());
		plug(cityView, profile.city());
		plug(selfieImage, profile.selfie());
	}


	private void hidePreferredNicknameWhenNeeded() {
		if (newContact) return;
		Observable.zip(profile.preferredNickname(), profile.ownName(), new Func2<String, String, Boolean>() {
			@Override
			public Boolean call(String preferredNickname, String ownName) {
				return preferredNickname.equalsIgnoreCase(ownName) || preferredNickname.equalsIgnoreCase(contact.nickname().current());
			}
		}).subscribe(new Action1<Boolean>() { @Override public void call(Boolean hidePreferredNickname) {
			if (hidePreferredNickname)
				preferredNicknameView.setVisibility(View.GONE);
		}});
	}


	private void validationOnTextChanged(final EditText editText) {
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (ownNameSubscription != null) ownNameSubscription.unsubscribe();
				if (preferredNicknameSubscription != null) preferredNicknameSubscription.unsubscribe();
				nicknameEdit.setError(sneer().problemWithNewNickname(party.publicKey().current(), editText.getText().toString()));
			}

			@Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
		});
	}


	private void saveContact() {
		final String nickName = nicknameEdit.getText().toString();
		try {
			if (newContact)
				sneer().addContact(nickName, party);

			if (!nickName.equals(intent.getExtras().getString(CURRENT_NICKNAME)))
				sneer().findContact(party).setNickname(nickName);
		} catch (FriendlyException e) {
			toast(e.getMessage());
		}
		toast("Contact saved");
	}


	@Override
	protected void onPause() {
		super.onPause();
		if (isOwn) return;
		saveContact();
	}


	private void toast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

}
