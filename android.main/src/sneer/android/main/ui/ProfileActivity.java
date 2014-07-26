package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.*;

import java.io.*;
import java.util.*;

import sneer.*;
import sneer.android.main.*;
import sneer.commons.exceptions.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.*;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.widget.*;

public class ProfileActivity extends Activity {

	static int TAKE_PICTURE = 1;
	
	ImageView selfieImage;
	View profileView;
	
	EditText firstNameEdit;
	EditText lastNameEdit;
	EditText preferredNickNameEdit;
	EditText countryEdit;
	EditText cityEdit;
	CheckBox privacyCheckBox;
	
	Bitmap bitMap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);
		
		profileView = View.inflate(this, R.layout.activity_profile, null);
		
		firstNameEdit = (EditText) profileView.findViewById(R.id.firstName);
		lastNameEdit = (EditText) profileView.findViewById(R.id.lastName);
		preferredNickNameEdit = (EditText) profileView.findViewById(R.id.preferredNickName);
		selfieImage = (ImageView) profileView.findViewById(R.id.selfie);
		countryEdit = (EditText) profileView.findViewById(R.id.country);
		cityEdit = (EditText) profileView.findViewById(R.id.city);
		privacyCheckBox = (CheckBox) profileView.findViewById(R.id.privacycheckBox);
		
		
		
		// Does your device have a camera?
        if(hasCamera()){
        	firstNameEdit.setBackgroundColor(0xFF00CC00);
        	firstNameEdit.setText("You have Camera");
        }
 
        // Do you have Camera Apps?
        if(hasDefualtCameraApp(MediaStore.ACTION_IMAGE_CAPTURE)){
        	lastNameEdit.setBackgroundColor(0xFF00CC00);
        	lastNameEdit.setText("You have Camera Apps");
        }
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.profile, menu);
		return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_save_profile:
			try {
				saveProfile();
			} catch (FriendlyException e) {
				toast(e.getMessage());
			}
			break;
		}

		return true;
	}
	
	@Override
	public void onContentChanged() {
		super.onContentChanged();
	}
	
	public void saveProfile() throws FriendlyException {
		toast("saving profile...");

		Profile profile = sneer().profileFor(sneer().self());
	
		String preferredNickname = preferredNickNameEdit.getText().toString();
		profile.setPreferredNickname(preferredNickname);
		
		String country = countryEdit.getText().toString();
		profile.setCountry(country);
		
		String city = cityEdit.getText().toString();
		profile.setCity(city);
		
		BitmapDrawable selfieDrawable = ((BitmapDrawable) selfieImage.getDrawable());
		Bitmap bitmap = selfieDrawable.getBitmap();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		byte[] imageInByte = stream.toByteArray();
		profile.setSelfie(imageInByte);
	}
	
	public void selfieOnClick(View v) {
		 toast("Selfie was clicked. :-)");
		 Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		 
		 File file = new File(Environment.getExternalStorageDirectory(), "selfie.jpg");
		 Uri photoPath = Uri.fromFile(file);
		 intent.putExtra(MediaStore.EXTRA_OUTPUT, photoPath);
		
		 startActivityForResult(intent, TAKE_PICTURE);
	}
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == TAKE_PICTURE && resultCode== RESULT_OK && intent != null){
            Bundle extras = intent.getExtras();
            bitMap = (Bitmap) extras.get("data");
            selfieImage.setImageBitmap(bitMap);
        }
    }
	
	 // method to check if you have a Camera
    private boolean hasCamera(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
 
    // method to check you have Camera Apps
    private boolean hasDefualtCameraApp(String action){
        final PackageManager packageManager = getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
 
        return list.size() > 0;
 
    }

	private void toast(String message) {
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		toast.show();
	}
	
}
