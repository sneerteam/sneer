package sneer.android.sendfiles;

import sneer.android.ui.MessageActivity;
import android.os.Bundle;

public class ViewSendFilesActivity extends MessageActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_send_files);
	}
}
