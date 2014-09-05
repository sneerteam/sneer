package sneer.android.voicemessage;

import java.io.*;

import android.app.*;
import android.media.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;


public class MainActivity extends Activity {

	static final String LOG_TAG = "----> Sneer VoiceMessage";
    static String mFileName = null;

    ImageButton mRecordButton;
    Button mPlayButton;
    TextView messageView;
    TextView lengthView;

    MediaRecorder mRecorder = null;
    MediaPlayer mPlayer = null;

    boolean mStartRecording = true;
    boolean mStartPlaying = true;

    private void onRecord(boolean start) {
        if (start) {
        	startRecording();
        } else {
        	stopRecording();
        }
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
        mFileName += "/audiorecordtest.3gp";
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_main);
        
        messageView = (TextView)findViewById(R.id.lblMessage);
        lengthView = (TextView)findViewById(R.id.lblLength);

        mRecordButton = (ImageButton)findViewById(R.id.mRecordButton);
        mRecordButton.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			onRecord(mStartRecording);
			if (mStartRecording) {
				messageView.setText("Recording...");
			} else {
				messageView.setText("Hold to record, release to send");
			}
			mStartRecording = !mStartRecording;
		}});
        
        mPlayButton = (Button)findViewById(R.id.mPlayButton);
        mPlayButton.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
        	onPlay(mStartPlaying);
            if (mStartPlaying)
                ((Button)v).setText("Stop playing");
            else
            	((Button)v).setText("Start playing");

            mStartPlaying = !mStartPlaying;
		}});
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

}
