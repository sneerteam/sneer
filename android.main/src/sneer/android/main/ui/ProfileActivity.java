package sneer.android.main.ui;

import sneer.android.main.*;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class ProfileActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profile);
	}
	
	@Override
	public void onContentChanged() {
		super.onContentChanged();
	}
	
	void addProfile(){
		View profileView = View.inflate(this, R.layout.activity_profile, null);
		final EditText firstNameEdit = (EditText) profileView.findViewById(R.id.firstName);
		final EditText lastNameEdit = (EditText) profileView.findViewById(R.id.lastName);
		final EditText nickNameEdit = (EditText) profileView.findViewById(R.id.nickname);
		final EditText countryEdit = (EditText) profileView.findViewById(R.id.country);
		final EditText cityEdit = (EditText) profileView.findViewById(R.id.city);
		final CheckBox privacyCheckBox = (CheckBox) profileView.findViewById(R.id.privacycheckBox);
	}
	
}
