package sneer.android.main.ui;

import static sneer.android.main.SneerApp.*;
import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.android.main.ui.utils.*;
import sneer.commons.exceptions.*;
import sneer.impl.keys.*;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;

public class ContactActivity extends Activity {

	static final String PARTY_PUK = "partyPuk";
	boolean newContact = false;
	Profile profile;
	ImageView selfieImage;
	byte[] selfieBytes;

	EditText nicknameEdit;
	TextView fullNameView;
	TextView preferredNickNameView;
	TextView countryView;
	TextView cityView;
	Party party;
	PublicKey partyPuk;
	private Contact contact;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (!SneerApp.checkOnCreate(this)) return;
		
		setContentView(R.layout.activity_contact);

		nicknameEdit = (EditText) findViewById(R.id.nickname);
		fullNameView = (TextView) findViewById(R.id.fullName);
		preferredNickNameView = (TextView) findViewById(R.id.preferredNickName);
		selfieImage = (ImageView) findViewById(R.id.selfie);
		countryView = (TextView) findViewById(R.id.country);
		cityView = (TextView) findViewById(R.id.city);

		load();
		getActionBar().setTitle(activityTitle());
		
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
		case R.id.action_share:
			Puk.sendYourPublicKey(ContactActivity.this, party);
			break;
		}

		return true;
	}
	

	private void load() {
		final Intent intent = getIntent();
		final String action = intent.getAction();
		
		if (Intent.ACTION_VIEW.equals(action)) 
			loadContact(Keys.createPublicKey(intent.getData().getQuery()));
		else
			loadProfile();
	}

	
	private String activityTitle() {
		Bundle extras = getIntent().getExtras();
		
		if (extras == null) {
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
		
		loadContact(null);
		
		nicknameEdit.setText(contact.nickname().current());
		
		profile.ownName().subscribe(new Action1<String>() { @Override public void call(String name) { 
			fullNameView.setText(name);
		}});

		profile.preferredNickname().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String preferredNickname) { 
			preferredNickNameView.setText("(" + preferredNickname+ ")");
		}});

		profile.country().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String country) {
			countryView.setText(country);
		}});

		profile.city().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String city) {
			cityView.setText(city);
		}});

		profile.selfie().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<byte[]>() {@Override public void call(byte[] selfie) {
			Bitmap bitmap = BitmapFactory.decodeByteArray(selfie, 0, selfie.length);
			selfieImage.setImageBitmap(bitmap);
		}});
	}

	private void loadContact(PublicKey puk){
		partyPuk = puk==null ? partyPuk() : puk;
		
		party = sneer().produceParty(partyPuk);
		profile = sneer().profileFor(party);
		contact = sneer().findContact(party);

		newContact = contact==null ? true : false; 		
	}
	
	
	@Override
	public void onContentChanged() {
		super.onContentChanged();
	}

	
	public void saveContact() {
		final String nickName = nicknameEdit.getText().toString();
		
		try {
			if (newContact)
				sneer().addContact(nickName, party);
			else
				sneer().findContact(party).setNickname(nickName);
			toast("contact saved...");
		} catch (FriendlyException e) {
			toast(e.getMessage());
		}
		
	}


	@Override
	protected void onStop() {
		saveContact();
		super.onStop();
	}

	
	private void toast(String message) {
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
		toast.show();
	}

	
	private void validationOnTextChanged(final EditText textView) {
		textView.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			public void afterTextChanged(Editable s) {
				nicknameEdit.setError(contact.problemWithNewNickname(textView.getText().toString()));
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
		});
	}
	
}