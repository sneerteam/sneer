package sneer.android.videomessage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import sneer.android.ui.MessageActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ViewVideoMessageActivity extends MessageActivity {

	static final File TEMP_VIDEO = new File(System.getProperty("java.io.tmpdir"), "videomessage.mp4");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_video_message);

		TEMP_VIDEO.deleteOnExit();
		
		playVideoMessage();
	}
	
	
	private void playVideoMessage() {
		byte[] message = (byte[])messagePayload();

		try {
			FileOutputStream fos = new FileOutputStream(TEMP_VIDEO);
			fos.write(message);
			fos.close();
			
			Uri videoUri = Uri.fromFile(TEMP_VIDEO);
			
			Intent intent = new Intent(Intent.ACTION_VIEW, videoUri);
			intent.setDataAndType(videoUri, "video/mp4");
			
	    	startActivity(intent);
		} catch (IOException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
		}
		finish();
	}
	
}
