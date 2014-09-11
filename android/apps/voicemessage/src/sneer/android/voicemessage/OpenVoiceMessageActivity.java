package sneer.android.voicemessage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import sneer.android.ui.SneerActivity;
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
	private volatile MediaPlayer player;

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

		btnPlay = (ImageButton)findViewById(R.id.btn_play);
		btnPause = (ImageButton)findViewById(R.id.btn_pause);

		btnPlay.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			btnPlay.setVisibility(View.INVISIBLE);
			btnPause.setVisibility(View.VISIBLE);
			OpenVoiceMessageActivity.this.setTitle("Playing Message");
			startPlaying(true);
		}});
		
		btnPause.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			btnPause.setVisibility(View.INVISIBLE);
			btnPlay.setVisibility(View.VISIBLE);
			OpenVoiceMessageActivity.this.setTitle("Paused");
			player.pause();
		}});
		
		seekbar = (SeekBar)findViewById(R.id.recording_progress_bar);
		recordingTime = (TextView)findViewById(R.id.recording_time);

		startPlaying(false);
	}


	private void startPlaying(boolean resume) {
		try {
			if (!resume) {
				player = new MediaPlayer();
				player.setOnCompletionListener(new OnCompletionListener() { @Override public void onCompletion(MediaPlayer mp) {
					finish();
				}});
				player.setDataSource(audioFileName);
				player.prepare();
				duration = player.getDuration();
				seekbar.setMax(duration);
			
				recordingTime.setText(String.format("%02d:%02d", 
				         TimeUnit.MILLISECONDS.toMinutes((long) duration),
				         TimeUnit.MILLISECONDS.toSeconds((long) duration)));
			
				myHandler.postDelayed(UpdateSongTime, 100);
			}
		} catch (IOException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		player.start();
	}

	
	private Runnable UpdateSongTime = new Runnable() { public void run() {
		if (player == null) return;
		position = player.getCurrentPosition();

		seekbar.setProgress((int) position);
			
		TimeUnit msec = TimeUnit.MILLISECONDS;
		TimeUnit min = TimeUnit.MINUTES;
			
		long currentDuration = player.getCurrentPosition();
		countDown = currentDuration - duration;
			
		recordingTime.setText(String.format("%02d:%02d", 
				msec.toMinutes((long) countDown),
				min.toSeconds(msec.toMinutes((long) countDown)) - msec.toSeconds((long) countDown)));

		myHandler.postDelayed(this, 100);
	}};

	private ImageButton btnPause;

	private ImageButton btnPlay;

	
	private void stopPlaying() {
		player.reset();
		player.release();
		player = null;
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
