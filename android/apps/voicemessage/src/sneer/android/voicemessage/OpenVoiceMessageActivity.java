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
	private volatile MediaPlayer player;

	private TextView recordingTime;
	private ImageButton btnPause;
	private ImageButton btnPlay;

	private SeekBar seekbar;
	private Handler handler = new Handler();

	private int duration;
	
	TimeUnit milliseconds = TimeUnit.MILLISECONDS;
	TimeUnit minutes = TimeUnit.MINUTES;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_open_voice_message);

		btnPlay = (ImageButton)findViewById(R.id.btn_play);
		btnPause = (ImageButton)findViewById(R.id.btn_pause);
		seekbar = (SeekBar)findViewById(R.id.recording_progress_bar);
		recordingTime = (TextView)findViewById(R.id.recording_time);
		
		btnPlay.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			invisible(btnPlay);
			visible(btnPause);
			activityTitle("Playing Message");
			start();
		}});
		
		btnPause.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			invisible(btnPause);
			visible(btnPlay);
			activityTitle("Paused");
			pause();
		}});		
	}

	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		play();
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
		stop();
	}
	
	
	private void activityTitle(String title) {
		OpenVoiceMessageActivity.this.setTitle(title);
	}


	private void visible(View v) {
		v.setVisibility(View.VISIBLE);
	}
	
	
	private void invisible(View v) {
		v.setVisibility(View.INVISIBLE);
	}


	private void initPlayer() throws IOException {
		player = new MediaPlayer();
		player.setOnCompletionListener(new OnCompletionListener() { @Override public void onCompletion(MediaPlayer mp) {
			finish();
		}});
		player.setDataSource(audioFileName);
		player.prepare();
	}

		
	private void play() {
		try {
			initPlayer();
			duration = player.getDuration();
			seekbar.setMax(duration);
		
			updateRecordingTime(duration, 0);
			
			postDelayed(update, 100);
		} catch (IOException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		start();
	}


	private void stop() {
		player.reset();
		player.release();
		player = null;
	}
	
	private void pause() {
		player.pause();
	}

	
	private void start() {
		player.start();
	}
	
	
	private Runnable update = new Runnable() { public void run() {
		if (player == null) return;
		final long position = player.getCurrentPosition();
		final long countDown = position - duration;
		
		seekbar.setProgress((int) position);
			
		updateRecordingTime(countDown, countDown);
	
		postDelayed(this, 100);
	}};

	private void updateRecordingTime(long v1, long v2) {
		recordingTime.setText(String.format("%02d:%02d", toMinutes(v1), minutesToSeconds(toMinutes(v1)) - toSeconds(v2)));
	}


	private void postDelayed(Runnable r, int delay) {
		handler.postDelayed(r, delay);
	}


	private long toSeconds(long v1) {
		return milliseconds.toSeconds(v1);
	}


	private long minutesToSeconds(long v1) {
		return minutes.toSeconds(v1);
	}


	private long toMinutes(long v1) {
		return milliseconds.toMinutes(v1);
	}
	
}
