package sneer.android.voicemessage;

import java.io.*;

import sneer.android.ui.*;
import android.media.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

public class MainActivity extends MessageActivity {

	static final String LOG_TAG = "----> Sneer VoiceMessage";
	static String mFileName = null;

	ImageButton mRecordButton;
	TextView lengthView;

	MediaRecorder mRecorder = null;
	MediaPlayer mPlayer = null;

	boolean mStartRecording = true;
	boolean mStartPlaying = true;

	int passedSenconds;
	private Thread t;

	
	private void onRecord(boolean start) {
		if (start) {
			startRecording();
			startTimer();
		} else {
			stopRecording();
			stopTimer();
		}
	}

	private void startTimer() {
		t = new Thread() {  @Override public void run() {
			passedSenconds = 0;
			try {
				while (!isInterrupted()) {
					Thread.sleep(1000);
					runOnUiThread(new Runnable() { @Override public void run() {
						MainActivity.this.setTitle(MainActivity.this.getTitle().toString() + ".");
						if (MainActivity.this.getTitle().toString().contains("...."))
							MainActivity.this.setTitle(MainActivity.this.getTitle().toString().replace("....", ""));

						int seconds = passedSenconds % 60;
						int minutes = (passedSenconds / 60) % 60;
						lengthView.setText(String.format("%02d : %02d", minutes, seconds));
						passedSenconds++;
					}});
				}
			} catch (InterruptedException e) {
				t.interrupt();
			}
		}};
		t.start();
	}
	
	private void stopTimer() {
		t.interrupt();
	}

	private void onPlay(boolean start) {
		if (start)
			startPlaying();
		else
			stopPlaying();
	}

	private void startPlaying() {
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(mFileName);
			mPlayer.prepare();
			mPlayer.start();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() failed");
		}
	}

	private void stopPlaying() {
		mPlayer.release();
		mPlayer = null;
	}

	private void startRecording() {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setOutputFile(mFileName);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e(LOG_TAG, "prepare() failed");
		}

		mRecorder.start();
	}

	private void stopRecording() {
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
	}

	public MainActivity() {
		mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
		mFileName += "/voicemessage.3gp";
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.activity_main);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
		}

		if (mPlayer != null) {
			mPlayer.release();
			mPlayer = null;
		}
	}

	@Override
	protected void composeMessage() {
		lengthView = (TextView) findViewById(R.id.lblLength);

		mRecordButton = (ImageButton) findViewById(R.id.mRecordButton);
		onRecord(mStartRecording);
	}

	
	protected byte[] recordingBytes() {
		byte[] ret = null;
		try {
			ret = readFully(this.openFileInput(mFileName));
		} catch (IOException e) {
			toast(e.getMessage());
		}
		return ret;
	}

	
	public static byte[] readFully(InputStream in) throws IOException {
		byte[] b = new byte[8192];
		int read;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
			while ((read = in.read(b)) != -1) {
				out.write(b, 0, read);
		}
		return out.toByteArray();
	}
	
	
	@Override
	protected void open(Object message) {

	}

}
