package fabiomanera.sendlocation;

import sneer.android.ui.*;
import android.content.*;
import android.content.DialogInterface.OnClickListener;
import android.os.*;

public class SendLocationActivity extends MessageActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final String[] options = new String[]{"here", "there"};
		alert("Send Location", options, new OnClickListener() {  @Override public void onClick(DialogInterface arg0, int option) {
			String location = options[option];
			send("Location: " + location, location);
			finish();
		} });
	}

}
