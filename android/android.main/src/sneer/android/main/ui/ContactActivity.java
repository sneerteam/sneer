package sneer.android.main.ui;

import static sneer.android.main.SneerApp.*;
import rx.*;
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
	private boolean isOwn;
	private boolean isTouched;
	
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
			Puk.sendYourPublicKey(ContactActivity.this, party, false, sneer().findContact(party).nickname().current());
			break;
		}
		return true;
	}
	

	private void load() {
		final Intent intent = getIntent();
		final String action = intent.getAction();
		
		if (Intent.ACTION_VIEW.equals(action)){
			try{
				loadContact(Keys.createPublicKey(intent.getData().getQuery()));		
			}catch(RuntimeException e){
				toast("Puk invalid format.");
				finish();
			}
			
		} else
			loadContact(null);
		
		if (partyPuk.bytesAsString().equals(sneer().self().publicKey().current().bytesAsString())) {
			isOwn = true;
			startActivity(new Intent().setClass(this, ProfileActivity.class));
			finish();
		} else if (!newContact){
			loadProfile();
		}
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
		nicknameEdit.setText(contact.nickname().current());
		
		profile.ownName().subscribe(new Action1<String>() { @Override public void call(String ownName) { 
			fullNameView.setText(ownName);
			
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
		
		
		Observable.zip(profile.preferredNickname(), profile.ownName(), new Func2<String, String, Boolean>(){ @Override public Boolean call(String preferredNickname, String ownName) {
			if(preferredNickname.equalsIgnoreCase(ownName) ||
					preferredNickname.equalsIgnoreCase(contact.nickname().current()))
				return true;
			else
				return false;
			}}).subscribe(new Action1<Boolean>(){ @Override public void call(Boolean validation) {
				if(validation)
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

		newContact = contact == null
				? true
				: false; 		
	}
	
	
	public void saveContact() {
		if (isTouched) {
			try {
				final String nickName = nicknameEdit.getText().toString();
				if (newContact)
					sneer().addContact(nickName, party);
				else
					sneer().findContact(party).setNickname(nickName);
				toast("contact saved...");
			} catch (FriendlyException e) {
				toast(e.getMessage());
			}
		}
	}

	
	@Override
	public void onContentChanged() {
		super.onContentChanged();
	}

	
	@Override
	protected void onStop() {
		if(!isOwn)
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
				if (!newContact) nicknameEdit.setError(contact.problemWithNewNickname(textView.getText().toString()));
				isTouched = true;
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
		});
	}
	
}