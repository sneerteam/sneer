package sneer.android.sendvideo;

import java.io.FileNotFoundException;

import sneer.android.ui.MessageActivity;
import sneer.commons.exceptions.FriendlyException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

public class ComposeVideoMessageActivity extends MessageActivity {

	static final int REQUEST_VIDEO_CAPTURE = 1;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_compose_video_message);

		takeVideo();
	}

	
	private void takeVideo() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		if (takeVideoIntent.resolveActivity(getPackageManager()) != null)
			startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
	    	send(data.getData());
	    }
        finish();
	}
	

	private void send(Uri videoUri) {		
		byte[] bytes = null;
		try {
			bytes = readFully((getContentResolver().openInputStream(videoUri)));
		} catch (FriendlyException e) {
			toast(e);
			finish();
		} catch (FileNotFoundException e) {
			toast(e);
			finish();
		}
		if (bytes != null)
			send("video message", bytes);
	}
	
}
