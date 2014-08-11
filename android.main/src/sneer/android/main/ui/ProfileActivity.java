package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.*;

import java.io.*;

import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.text.*;
import android.view.*;
import android.widget.*;

public class ProfileActivity extends Activity {

	static final String PARTY_PUK = "partyPuk";
	static final int TAKE_PICTURE = 1;
	static final int THUMBNAIL_SIZE = 128;

	ImageView selfieImage;
	
	Profile profile;
	
	EditText firstNameEdit;
	EditText lastNameEdit;
	EditText preferredNickNameEdit;
	EditText countryEdit;
	EditText cityEdit;
	private byte[] selfieBytes;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);
		
		PublicKey partyPuk = partyPuk();
		Party party = partyPuk == null
			? sneer().self()
			: sneer().produceParty(partyPuk);
		
		profile = sneer().profileFor(party);

		firstNameEdit = (EditText) findViewById(R.id.firstName);
		lastNameEdit = (EditText) findViewById(R.id.lastName);
		preferredNickNameEdit = (EditText) findViewById(R.id.preferredNickName);
		selfieImage = (ImageView) findViewById(R.id.selfie);
		countryEdit = (EditText) findViewById(R.id.country);
		cityEdit = (EditText) findViewById(R.id.city);
		
		if (partyPuk != null) {
			readonly();
			preferredNickNameEdit.setHint("nickname");
		}
		
		afterTextChanged(firstNameEdit);
		afterTextChanged(lastNameEdit);
		
		loadProfile();
	}


	private PublicKey partyPuk() {
		Bundle extras = getIntent().getExtras();
		if (extras == null) return null;
		
		return (PublicKey)extras.getSerializable(PARTY_PUK);
	}


	private void readonly() {
		firstNameEdit.setEnabled(false);
		lastNameEdit.setEnabled(false);
		selfieImage.setEnabled(false);
		countryEdit.setEnabled(false);
		cityEdit.setEnabled(false);
	}


	private void afterTextChanged(final EditText textView) {
				
		textView.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			public void afterTextChanged(Editable s) {
				checkMoreThanOneCharacter(textView);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
		});
	}
	
	
	private void loadProfile() {
		profile.name().subscribe(new Action1<String>() { @Override public void call(String name) {
			if (name != null && !name.trim().isEmpty()) {
				firstNameEdit.setText(name);
				firstNameEdit.setHint(R.string.profile_view_full_name);
				lastNameEdit.setVisibility(View.GONE);
			} else {
				firstNameEdit.setHint(R.string.profile_view_first_name);
				lastNameEdit.setVisibility(View.VISIBLE);
			}
		}});
		
		profile.preferredNickname().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String preferredNickname) {
			preferredNickNameEdit.setText(preferredNickname);
		}});
		
		profile.country().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String country) {
			countryEdit.setText(country);
		}});
		
		profile.city().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String city) {
			cityEdit.setText(city);
		}});
		
		profile.selfie().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<byte[]>() { @Override public void call(byte[] selfie) {
			Bitmap bitmap = BitmapFactory.decodeByteArray(selfie, 0, selfie.length);
			selfieImage.setImageBitmap(bitmap);
		}});
	}


	@Override
	public void onContentChanged() {
		super.onContentChanged();
	}
	
	
	public void saveProfile() {
		if (firstNameEdit.getText().toString().trim().length() > 1 && lastNameEdit.getText().toString().trim().length() > 1) 
			profile.setName(firstNameEdit.getText() + " " + lastNameEdit.getText());
		
		String preferredNickname = preferredNickNameEdit.getText().toString();
		profile.setPreferredNickname(preferredNickname);
		
		String country = countryEdit.getText().toString();
		profile.setCountry(country);
		
		String city = cityEdit.getText().toString();
		profile.setCity(city);
		
		if (selfieBytes != null)
			profile.setSelfie(selfieBytes);

		toast("profile saved...");
	}
	
	
	public void selfieOnClick(View v) {
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
		galleryIntent.setType("image/*");
		
		Intent chooser = Intent.createChooser(galleryIntent, "Open with");
		chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

		startActivityForResult(chooser, TAKE_PICTURE);
	}
	
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)  {
		if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK && data != null){
	        int size = THUMBNAIL_SIZE;
	        Bitmap bitMap;

			if (data.getExtras() != null && data.getExtras().get("data") != null) {
				bitMap = (Bitmap) data.getExtras().get("data");
			} else if (data.getData() != null) {			
				Uri selectedImage = data.getData();
				try {
					bitMap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage)), size, size);
					
				} catch (FileNotFoundException e) {
					toast("Error loading " + selectedImage);
					return;
				}
			} else {
				toast("Error selecting image source.");
				return;
			}

			do {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				bitMap.compress(Bitmap.CompressFormat.JPEG, 90, out);
				selfieBytes = out.toByteArray();
				size = (int) (size * 0.9f);
			} while (selfieBytes.length > 1024 * 10);
	        
	        toast("size: " + selfieBytes.length);	        
	        selfieImage.setImageBitmap(BitmapFactory.decodeByteArray(selfieBytes, 0, selfieBytes.length));
		}		
    }
	

    @Override
    protected void onStop() {
    	if (profile == self())
    		saveProfile();
		super.onStop();
    }


	private Profile self() {
		return sneer().profileFor(sneer().self());
	}
    
    
    private void toast(String message) {
    	Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
    	toast.show();
    }

    
	public void checkMoreThanOneCharacter(EditText edt) {
		if (edt.getText().toString().trim().length() <= 1)
			edt.setError("Name too short");
	}

	
}
