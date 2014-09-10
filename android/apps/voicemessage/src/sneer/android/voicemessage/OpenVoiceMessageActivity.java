package sneer.android.voicemessage;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class OpenVoiceMessageActivity extends Activity {

	static final String LOG_TAG = "----> Sneer VoiceMessage";
	
	private final String audioFileName = new File(System.getProperty("java.io.tmpdir"), "voicemessage.3gp").getAbsolutePath();
	private MediaPlayer mPlayer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_open_voice_message);

		startPlaying();
	}


	private void startPlaying() {
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(audioFileName);
			mPlayer.prepare();
			mPlayer.start();
		} catch (IOException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	
	private void stopPlaying() {
		mPlayer.release();
		mPlayer = null;
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
		stopPlaying();
	}
}
