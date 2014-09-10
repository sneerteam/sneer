package sneer.android.voicemessage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class OpenVoiceMessageActivity extends Activity {

	static final String LOG_TAG = "----> Sneer VoiceMessage";

	private final String audioFileName = new File(System.getProperty("java.io.tmpdir"), "voicemessage.3gp").getAbsolutePath();
	private MediaPlayer mPlayer;

	private TextView recordingTime;
	private SeekBar seekbar;
	private Handler myHandler = new Handler();

	private int duration;
	private int position;
	private long countDown;	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_open_voice_message);

		final ImageButton btnPlay = (ImageButton)findViewById(R.id.btn_play);
		final ImageButton btnPause = (ImageButton)findViewById(R.id.btn_pause);

		btnPlay.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			btnPlay.setVisibility(View.INVISIBLE);
			btnPause.setVisibility(View.VISIBLE);
			OpenVoiceMessageActivity.this.setTitle("Playing Message");
			startPlaying(true);
			Toast.makeText(OpenVoiceMessageActivity.this, "clicked play", Toast.LENGTH_SHORT).show();
		}});
		
		btnPause.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			btnPause.setVisibility(View.INVISIBLE);
			btnPlay.setVisibility(View.VISIBLE);
			OpenVoiceMessageActivity.this.setTitle("Paused");
			mPlayer.pause();
			Toast.makeText(OpenVoiceMessageActivity.this, "clicked pause", Toast.LENGTH_SHORT).show();
		}});
		
		seekbar = (SeekBar)findViewById(R.id.recording_progress_bar);
		recordingTime = (TextView)findViewById(R.id.recording_time);
		

		startPlaying(false);
	}


	private void startPlaying(boolean resume) {
		try {
			if (!resume) {
				mPlayer = new MediaPlayer();
				mPlayer.setOnCompletionListener(new OnCompletionListener() { @Override public void onCompletion(MediaPlayer mp) {
					Toast.makeText(OpenVoiceMessageActivity.this, "finish him", Toast.LENGTH_SHORT).show();
				}});
				mPlayer.setDataSource(audioFileName);
				mPlayer.prepare();
				duration = mPlayer.getDuration();
				seekbar.setMax(duration);
			
				recordingTime.setText(String.format("%02d:%02d", 
				         TimeUnit.MILLISECONDS.toMinutes((long) duration),
				         TimeUnit.MILLISECONDS.toSeconds((long) duration)));
			
				myHandler.postDelayed(UpdateSongTime, 100);
			}
		} catch (IOException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		mPlayer.start();
	}

	
	private Runnable UpdateSongTime = new Runnable() { public void run() {
		position = mPlayer.getCurrentPosition();

		seekbar.setProgress((int) position);
			
		TimeUnit milliSeconds = TimeUnit.MILLISECONDS;
		TimeUnit minutes = TimeUnit.MINUTES;
			
		long currentDuration = mPlayer.getCurrentPosition();
		countDown = currentDuration - duration;
			
		recordingTime.setText(String.format("%02d:%02d", 
				milliSeconds.toMinutes((long) countDown),
				minutes.toSeconds(milliSeconds.toMinutes((long) countDown)) - milliSeconds.toSeconds((long) countDown)));

		myHandler.postDelayed(this, 100);
	}};

	
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
