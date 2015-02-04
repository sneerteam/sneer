package sneer.android.videomessage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import sneer.android.ui.MessageActivity;
import sneer.commons.exceptions.FriendlyException;

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
			send("video message", bytes, null);
	}

    private void toast(Exception e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG);
    }


    public byte[] readFully(InputStream inputStream) throws FriendlyException {
        byte[] b = new byte[8192];
        int read;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            while ((read = inputStream.read(b)) != -1) {
                out.write(b, 0, read);
            }
        } catch (IOException e) {
            throw new FriendlyException("Failed to read file");
        }
        return out.toByteArray();
    }

}
