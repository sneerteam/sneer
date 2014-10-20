package sneer.android.main.ui;

import static sneer.android.main.ui.SneerAndroidProvider.sneer;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import sneer.Profile;
import sneer.android.main.R;
import sneer.android.ui.SneerActivity;
import sneer.commons.exceptions.FriendlyException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class ProfileActivity extends SneerActivity {

	private static final int TAKE_PICTURE = 1;

	private Profile profile;
	
	private EditText firstNameEdit;
	private EditText lastNameEdit;
	private EditText preferredNickNameEdit;
	private EditText countryEdit;
	private EditText cityEdit;
	private ImageView selfieImage;
	
	private byte[] selfieBytes;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);

		profile = sneer().profileFor(sneer().self());

		firstNameEdit = (EditText) findViewById(R.id.firstName);
		lastNameEdit = (EditText) findViewById(R.id.lastName);
		preferredNickNameEdit = (EditText) findViewById(R.id.preferredNickName);
		selfieImage = (ImageView) findViewById(R.id.selfie);
		countryEdit = (EditText) findViewById(R.id.country);
		cityEdit = (EditText) findViewById(R.id.city);
		
		afterTextChanged(firstNameEdit);
		afterTextChanged(lastNameEdit);
		
		loadProfile();
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
	        return true;		
		}
		return true;
	}
	

	private void afterTextChanged(final EditText textView) {
		textView.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			public void afterTextChanged(Editable s) {
				checkNameLength(textView);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		});
	}
	
	
	private void loadProfile() {
		plugOwnName(firstNameEdit, lastNameEdit, profile.ownName());
		plug(preferredNickNameEdit, profile.preferredNickname());
		plug(countryEdit, profile.country());
		plug(cityEdit, profile.city());
		plug(selfieImage, profile.selfie());
	}


	@Override
	public void onContentChanged() {
		super.onContentChanged();
	}
	
	
	public void saveProfile() {
		String preferredNickname = text(preferredNickNameEdit);
		profile.setPreferredNickname(preferredNickname);
		
		String country = text(countryEdit);
		profile.setCountry(country);
		
		String city = text(cityEdit);
		profile.setCity(city);
		
		if (selfieBytes != null)
			profile.setSelfie(selfieBytes);

		if (lastNameEdit.getVisibility() == View.GONE) {
			if (text(firstNameEdit).length() > 1) 
				setOwnName(text(firstNameEdit));
		} else {
			if (text(firstNameEdit).length() > 1 && text(lastNameEdit).length() > 1) 
				setOwnName(text(firstNameEdit) + " " + text(lastNameEdit));
		}
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
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)  {
		if (requestCode != TAKE_PICTURE) return;
		if (resultCode != RESULT_OK) return;
		if (intent == null) return;
		
        Bitmap bitmap;
		try {
			bitmap = loadBitmap(intent);
		} catch (FriendlyException e) {
			toast(e);
			return;
		}

		selfieBytes = scaledDownTo(bitmap, 10 * 1024);
        selfieImage.setImageBitmap(toBitmap(selfieBytes));
    }


	@Override
	protected void onPause() {
		super.onPause();
    	saveProfile();
	}
	
    
	public void checkNameLength(EditText edit) {
		if (text(edit).length() <= 1)
			edit.setError("Name too short");
	}

	public static Subscription plugOwnName(final TextView textView1, final TextView textView2, Observable<?> observable) {
		return deferUI(observable).subscribe(new Action1<Object>() { @Override public void call(Object obj) {
			if (obj.toString() != null && !obj.toString().trim().isEmpty()) {
				textView1.setText(obj.toString());
				textView2.setVisibility(View.GONE);
			} else {
				textView2.setVisibility(View.VISIBLE);
			}
		}});
	}


	private void setOwnName(String name) {
		profile.setOwnName(name);
		toast("Profile saved");
	}


	private String text(EditText editText) {
		return editText.getText().toString().trim();
	}
	
}
