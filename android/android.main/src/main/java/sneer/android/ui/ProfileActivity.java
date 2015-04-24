package sneer.android.ui;

import android.app.ActionBar;
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

import sneer.main.R;
import rx.functions.Action1;
import sneer.Profile;
import sneer.commons.exceptions.FriendlyException;

import static sneer.android.SneerAndroidSingleton.sneer;

public class ProfileActivity extends SneerActivity {

	private static final int TAKE_PICTURE = 1;

	private Profile profile;

	private EditText firstNameEdit;
	private EditText lastNameEdit;
	private EditText preferredNickNameEdit;
	private EditText countryEdit;
	private EditText cityEdit;
	private ImageView selfieImage;

	private static String ownName;
	private static String preferredNickname;
	private static String country;
	private static String city;
	private static Bitmap selfie;

	private byte[] selfieBytes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

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


	private void afterTextChanged(final EditText editText) {
		editText.addTextChangedListener(new TextWatcher() {
			@Override public void afterTextChanged(Editable s) {
				checkNameLength(editText);
			}

			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
		});
	}


	private void loadProfile() {
		onMainThread(profile.ownName()).subscribe(new Action1<String>() { @Override public void call(String ownName) {
			ProfileActivity.ownName = ownName;
			firstNameEdit.setText(ownName);
			lastNameEdit.setVisibility(View.GONE);
		}});

		onMainThread(profile.preferredNickname()).subscribe(new Action1<String>() { @Override public void call(String preferredNickname) {
			ProfileActivity.preferredNickname = preferredNickname;
			preferredNickNameEdit.setText(preferredNickname);
		}});

		onMainThread(profile.country()).subscribe(new Action1<String>() { @Override public void call(String country) {
			ProfileActivity.country = country;
			countryEdit.setText(country);
		}});

		onMainThread(profile.city()).subscribe(new Action1<String>() { @Override public void call(String city) {
			ProfileActivity.city = city;
			cityEdit.setText(city);
		}});

		onMainThread(profile.selfie().map(TO_BITMAP)).subscribe(new Action1<Bitmap>() { @Override public void call(Bitmap selfie) {
			ProfileActivity.selfie = selfie;
			selfieImage.setImageBitmap(selfie);
		}});
	}


	public void saveProfile() {
		boolean changed = false;

		if (text(firstNameEdit).length() < 2) return;

		if (!text(firstNameEdit).equals(ownName)) {
			if (lastNameEdit.getVisibility() == View.GONE) {
				profile.setOwnName(text(firstNameEdit));
				changed = true;
			} else {
				if (text(lastNameEdit).length() < 2) return;
				profile.setOwnName(text(firstNameEdit) + " " + text(lastNameEdit));
				changed = true;
			}
		}

		if (!text(preferredNickNameEdit).equals(preferredNickname)) {
			profile.setPreferredNickname(text(preferredNickNameEdit));
			changed = true;
		}

		if (!text(countryEdit).equals(country)) {
			profile.setCountry(text(countryEdit));
			changed = true;
		}

		if (!text(cityEdit).equals(city)) {
			profile.setCity(text(cityEdit));
			changed = true;
		}

		if (selfieBytes != null) {
			profile.setSelfie(selfieBytes);
			changed = true;
		}

		if (changed)
			toast("Profile saved");
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
		saveProfile();
		super.onPause();
	}


	public void checkNameLength(EditText edit) {
		edit.setError(text(edit).length() == 1
			? "Name too short"
			: null); //Setting null necessary because of a bug on some Android versions.
	}


	private String text(EditText editText) {
		return editText.getText().toString().trim();
	}

}
