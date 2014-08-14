package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.*;
import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import android.app.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class ContactActivity extends Activity {

	static final String PARTY_PUK = "partyPuk";
	static final int TAKE_PICTURE = 1;
	static final int THUMBNAIL_SIZE = 128;

	ImageView selfieImage;

	Profile profile;

	TextView fullName;
	TextView preferredNickName;
	EditText nickNameEdit;
	TextView country;
	TextView city;
	private byte[] selfieBytes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact);

		PublicKey partyPuk = partyPuk();
		Party party = sneer().produceParty(partyPuk);

		profile = sneer().profileFor(party);

		fullName = (TextView) findViewById(R.id.fullName);
		preferredNickName = (EditText) findViewById(R.id.preferredNickName);
		selfieImage = (ImageView) findViewById(R.id.selfie);
		country = (TextView) findViewById(R.id.country);
		city = (TextView) findViewById(R.id.city);

		loadProfile();

	}

	private PublicKey partyPuk() {
		Bundle extras = getIntent().getExtras();
		if (extras == null)
			return null;

		return (PublicKey) extras.getSerializable(PARTY_PUK);
	}

	private void loadProfile() {

		profile.preferredNickname().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String preferredNickname) {
			preferredNickName.setText(preferredNickname);
		}});

		profile.selfie().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<byte[]>() { @Override public void call(byte[] selfie) {
			Bitmap bitmap = BitmapFactory.decodeByteArray(selfie, 0, selfie.length);
			selfieImage.setImageBitmap(bitmap);					
		}});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.contact, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void toast(String message) {
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
		toast.show();
	}

	public void saveContact() {

		String editNickName = nickNameEdit.getText().toString();
		// profile. (preferredNickname);

		toast("profile saved...");
	}
}
