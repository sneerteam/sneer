package sneer.android.main.ui;

import static sneer.android.main.SneerSingleton.*;

import java.io.*;

import rx.android.schedulers.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.commons.exceptions.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.*;
import android.view.*;
import android.widget.*;

public class ProfileActivity extends Activity {

	static int TAKE_PICTURE = 1;
	
	ImageView selfieImage;
	View profileView;
	
	Profile profile;
	
	EditText firstNameEdit;
	EditText lastNameEdit;
	EditText preferredNickNameEdit;
	EditText countryEdit;
	EditText cityEdit;
	
	Bitmap bitMap;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);
		
		profile = sneer().profileFor(sneer().self());

		profileView = View.inflate(this, R.layout.activity_profile, null);
		
		firstNameEdit = (EditText) profileView.findViewById(R.id.firstName);
		lastNameEdit = (EditText) profileView.findViewById(R.id.lastName);
		preferredNickNameEdit = (EditText) profileView.findViewById(R.id.preferredNickName);
		selfieImage = (ImageView) profileView.findViewById(R.id.selfie);
		countryEdit = (EditText) profileView.findViewById(R.id.country);
		cityEdit = (EditText) profileView.findViewById(R.id.city);
		
		loadProfile();
	}
	
	
	private void loadProfile() {
		firstNameEdit.setText("this is not working");
		
		profile.preferredNickname().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String preferredNickname) {
			preferredNickNameEdit.setText(preferredNickname);
		}});
		
		profile.country().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String country) {
			countryEdit.setText(country);
		}});
		
		profile.city().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() { @Override public void call(String city) {
			cityEdit.setText(city);
		}});
		
//		profile.selfie().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<byte[]>() { @Override public void call(byte[] selfie) {
//			BitmapFactory.Options options = new BitmapFactory.Options();
//			Bitmap bitmap = BitmapFactory.decodeByteArray(selfie, 0, selfie.length);
//			selfieImage.setImageBitmap(bitmap);
//		}});
	}


	@Override
	public void onContentChanged() {
		super.onContentChanged();
	}
	
	
	public void saveProfile() throws FriendlyException {
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

		toast("profile saved...");
	}
	
	
	public void selfieOnClick(View v) {
		if (hasCamera()) {
			toast("Selfie was clicked. :-)");
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			startActivityForResult(intent, TAKE_PICTURE);
		} else {
			toast("Your phone doesn't have a camera. :-(");
		}
	}
	
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == TAKE_PICTURE && resultCode== RESULT_OK && intent != null){
			Bundle extras = intent.getExtras();
			bitMap = (Bitmap)extras.get("data");

//			 TODO setImageBitmap not working
			selfieImage.setImageBitmap(bitMap);
		}
    }
	

	private boolean hasCamera(){
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
 
    
    @Override
    protected void onStop() {
        super.onStop();
        try {
			saveProfile();
		} catch (FriendlyException e) {
			toast(e.getMessage());
		}
    }
    
    
    @Override
    protected void onResume() {
    	super.onResume();
    	LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//    	 TODO Redraw UI here to update EditTexts text.
    }

    
    private void toast(String message) {
    	Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
    	toast.show();
    }
	
}
