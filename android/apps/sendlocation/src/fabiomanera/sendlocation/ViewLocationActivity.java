package fabiomanera.sendlocation;

import sneer.android.ui.*;
import android.content.*;
import android.content.DialogInterface.*;
import android.os.*;

public class ViewLocationActivity extends MessageActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Object location = message();
		
		alert("Location: " + location, new String[] {"OK"}, new OnClickListener() {  @Override public void onClick(DialogInterface arg0, int option) {
			finish();
		} });
	}
	
}
