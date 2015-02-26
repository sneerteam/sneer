package sneer.android.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import sneer.Contact;
import sneer.Party;
import sneer.Profile;
import sneer.PublicKey;
import sneer.android.R;
import sneer.android.utils.Puk;
import sneer.commons.exceptions.FriendlyException;

import static sneer.android.SneerAndroidSingleton.*;
import static sneer.android.ui.SneerActivity.*;

public class ContactActivity extends Activity {

	static final String PARTY_PUK = "partyPuk";
	static final String CURRENT_NICKNAME = "currentNickname";

    private ActionBar actionBar;

	private boolean newContact = false;
	private Profile profile;
	private ImageView selfieImage;

	private EditText nicknameEdit;
	private TextView fullNameView;
	private TextView preferredNickNameView;
	private TextView countryView;
	private TextView cityView;
	private Party party;
	private PublicKey partyPuk;
	private Contact contact;
	private boolean isOwn;
	private Subscription ownNameSubscription;
    private Subscription preferredNicknameSubscription;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (!sneerAndroid().checkOnCreate(this)) return;

		setContentView(R.layout.activity_contact);

		nicknameEdit = (EditText) findViewById(R.id.nickname);
		fullNameView = (TextView) findViewById(R.id.fullName);
		preferredNickNameView = (TextView) findViewById(R.id.preferredNickName);
		selfieImage = (ImageView) findViewById(R.id.selfie);
		countryView = (TextView) findViewById(R.id.country);
		cityView = (TextView) findViewById(R.id.city);

		load();

        actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setTitle(activityTitle());

		validationOnTextChanged(nicknameEdit);
    }


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.profile, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
	        return true;

		case R.id.action_share:
            if (!newContact)
			    Puk.sendYourPublicKey(ContactActivity.this, party, false, sneer().findContact(party).nickname().current());
			break;
		}
		return true;
	}


	private void load() {
		final Intent intent = getIntent();
		final String action = intent.getAction();

		if (Intent.ACTION_VIEW.equals(action)){
			try {
                if (actionBar != null)
				    actionBar.setDisplayHomeAsUpEnabled(true);
				loadContact(admin().keys().createPublicKey(intent.getData().getQuery()));
			} catch (RuntimeException e) {
				toast("Invalid public key");
				finish();
				return;
			}
		} else {
			loadContact(null);
		}

		if (partyPuk.toHex().equals(sneer().self().publicKey().current().toHex())) {
			isOwn = true;
			startActivity(new Intent().setClass(this, ProfileActivity.class));
			finish();
		} else {
			loadProfile();
		}
	}

	private String activityTitle() {
		if (getIntent().getExtras().get(PARTY_PUK) == null) {
			newContact = true;
			return "New Contact";
		}

		return "Contact";
	}


	private PublicKey partyPuk() {
		Bundle extras = getIntent().getExtras();
		if (extras == null)
			return null;

		return (PublicKey) extras.getSerializable(PARTY_PUK);
	}

	private void loadProfile() {
		if (newContact) {
            ownNameSubscription = plug(nicknameEdit, profile.ownName());
            if (nicknameEdit.getText().toString().isEmpty()) {
                preferredNicknameSubscription = plug(nicknameEdit, profile.preferredNickname());
            }
        } else {
            plug(nicknameEdit, contact.nickname().observable());
		}

		plug(fullNameView, profile.ownName());
		plug(preferredNickNameView, profile.preferredNickname().map(new Func1<Object, String>() { @Override public String call(Object obj) {
			return "(" + obj.toString() + ")";
		}}));
		plug(countryView, profile.country());
		plug(cityView, profile.city());
		plug(selfieImage, profile.selfie());

		if (!newContact)
			Observable.zip(profile.preferredNickname(), profile.ownName(), new Func2<String, String, Boolean>(){ @Override public Boolean call(String preferredNickname, String ownName) {
				if (preferredNickname.equalsIgnoreCase(ownName) || preferredNickname.equalsIgnoreCase(contact.nickname().current()))
					return true;
				else
					return false;
				}}).subscribe(new Action1<Boolean>(){ @Override public void call(Boolean validation) {
					if (validation)
						preferredNickNameView.setVisibility(View.GONE);
				}});

	}

	private void loadContact(PublicKey puk){
		partyPuk = puk == null
				? partyPuk()
				: puk;

		party = sneer().produceParty(partyPuk);
		profile = sneer().profileFor(party);
		contact = sneer().findContact(party);

		newContact = contact == null;
	}


	private void saveContact() {
		final String nickName = nicknameEdit.getText().toString();
		try {
			if (newContact)
				sneer().addContact(nickName, party);

			if (!nickName.equals(getIntent().getExtras().getString(CURRENT_NICKNAME)))
				sneer().findContact(party).setNickname(nickName);
		} catch (FriendlyException e) {
			toast(e.getMessage());
		}
		toast("Contact saved");
	}


	@Override
	public void onContentChanged() {
		super.onContentChanged();
	}


	@Override
	protected void onPause() {
		if (!isOwn)
			saveContact();
		super.onPause();
	}


	private void toast(String message) {
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
		toast.show();
	}


	private void validationOnTextChanged(final EditText editText) {
		editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
				if (ownNameSubscription != null) ownNameSubscription.unsubscribe();
				if (preferredNicknameSubscription != null) preferredNicknameSubscription.unsubscribe();
                nicknameEdit.setError(sneer().problemWithNewNickname(party.publicKey().current(), editText.getText().toString()));
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        });
	}

}
