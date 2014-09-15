package sneer.android.sendfiles;

import sneer.android.ui.MessageActivity;
import android.content.Intent;
import android.os.Bundle;

public class SendFilesActivity extends MessageActivity {

	private static final int PICKFILE_RESULT_CODE = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		if (message() != null) {
//			open(message());
//		} else {
//			composeMessage();
//		}
		open(message());
	}

	private void composeMessage() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("file/*");
		startActivityForResult(intent, PICKFILE_RESULT_CODE);
	}

	private void open(Object message) {
		navigateTo(ViewSendFilesActivity.class);
	}

	@Override
	public void onPause() {
		super.onPause();
		finish();
	}

	@Override
	protected void onStop() {
		super.onStop();
		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
