package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.*;
import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.commons.exceptions.*;
import sneer.impl.keys.*;
import android.app.*;
import android.graphics.*;
import android.os.*;
import android.widget.*;

public class ContactActivity extends Activity {

	static final String PARTY_PUK = "partyPuk";
	static final String ACTIVITY_TITLE = "activityTitle";
	static final int TAKE_PICTURE = 1;
	static final int THUMBNAIL_SIZE = 128;

	boolean newContact = false;

	Profile profile;

	ImageView selfieImage;

	byte[] selfieBytes;

	EditText nicknameEdit;
	EditText publicKeyEdit;
	TextView fullNameView;
	TextView preferredNickNameView;
	TextView countryView;
	TextView cityView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact);

		PublicKey partyPuk;
		Party party;

		getActionBar().setTitle(activityTitle());

		nicknameEdit = (EditText) findViewById(R.id.nickname);
		fullNameView = (TextView) findViewById(R.id.fullName);
		publicKeyEdit = (EditText) findViewById(R.id.publicKey);
		preferredNickNameView = (TextView) findViewById(R.id.preferredNickName);
		selfieImage = (ImageView) findViewById(R.id.selfie);
		countryView = (TextView) findViewById(R.id.country);
		cityView = (TextView) findViewById(R.id.city);

		readonly();

		if (!(newContact)) {
			partyPuk = partyPuk();
			party = sneer().produceParty(partyPuk);
			profile = sneer().profileFor(party);
			loadProfile();
		}
	}

	private String activityTitle() {
		Bundle extras = getIntent().getExtras();
		if (extras == null) {
			newContact = true;
			return "New Contact";
		}

		return (String) extras.getSerializable(ACTIVITY_TITLE);
	}

	private PublicKey partyPuk() {
		Bundle extras = getIntent().getExtras();
		if (extras == null)
			return null;

		return (PublicKey) extras.getSerializable(PARTY_PUK);
	}

	private void readonly() {
		fullNameView.setEnabled(false);
		preferredNickNameView.setEnabled(false);
		selfieImage.setEnabled(false);
		countryView.setEnabled(false);
		cityView.setEnabled(false);
	}

	private void loadProfile() {
		profile.ownName().subscribe(new Action1<String>() {
			@Override
			public void call(String name) {
				fullNameView.setText(name);
			}
		});

		profile.preferredNickname().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String preferredNickname) { 
			preferredNickNameView.setText("(" + preferredNickname+ ")");
		}});

		profile.country().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String country) {
			countryView.setText(country);
		}});

		profile.city().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String city) {
			cityView.setText(city);
		}});

		profile.selfie().observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Action1<byte[]>() {
					@Override
					public void call(byte[] selfie) {
						Bitmap bitmap = BitmapFactory.decodeByteArray(selfie,
								0, selfie.length);
						selfieImage.setImageBitmap(bitmap);
					}
				});
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();
	}

	public void saveContact() {
		
		String contactPublicKey = publicKeyEdit.getText().toString();
		final String nickName = nicknameEdit.getText().toString();
		
		Party party = sneer().produceParty(Keys.createPublicKey(contactPublicKey.getBytes()));	
		
		if(newContact){
			try{
				sneer().addContact(nickName, party);				
				toast("contact saved...");
			} catch (FriendlyException e) {
				toast(e.getMessage());
			}
			return;
		}
		
		sneer().findContact(party).map(toWritableContact()).subscribe(new Action1<WritableContact>() {  @Override public void call(WritableContact writableContact) {
			try {
				writableContact.setNickname(nickName);
				toast("contact saved...");
			} catch (FriendlyException e) {
				toast(e.getMessage());
			}	
		} });
		
	}

	private Func1<Contact, WritableContact> toWritableContact() {
		return new Func1<Contact, WritableContact>() {  @Override public WritableContact call(Contact t1) {
			return sneer().writable(t1);
		}};
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

}