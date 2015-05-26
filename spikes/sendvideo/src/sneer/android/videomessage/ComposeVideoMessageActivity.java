package sneer.android.videomessage;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import sneer.android.ui.MessageActivity;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
		} catch (FileNotFoundException e) {
			toast(e.getMessage());
			finish();
		} catch (IOException e) {
            toast("Unable to read file");
			finish();
		}
		if (bytes != null)
			send("video message", bytes, null);
	}

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }


    public byte[] readFully(InputStream inputStream) throws IOException {
        byte[] b = new byte[8192];
        int read;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while ((read = inputStream.read(b)) != -1)
            out.write(b, 0, read);
        return out.toByteArray();
    }

}
