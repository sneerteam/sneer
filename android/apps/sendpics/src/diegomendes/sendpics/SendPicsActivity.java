package diegomendes.sendpics;

import sneer.android.ui.*;
import sneer.commons.exceptions.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.*;
import android.widget.*;

public class SendPicsActivity extends MessageActivity {

	private static final int TAKE_PICTURE = 1;
	
	ImageView image;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_pics);
        
        image = (ImageView) findViewById(R.id.picture);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)  {
    	if (requestCode != TAKE_PICTURE) return;
		if (resultCode != RESULT_OK) return;
		if (intent == null) return;
		
        Bitmap bitmap;
		try {
			bitmap = loadBitmap(intent);
		} catch (FriendlyException e) {
			toast(e);
			return;
		}

		byte[] imageBytes = scaledDownTo(bitmap, 40 * 1024);
		send(imageBytes);
    }
    

	@Override
	protected void composeMessage() {
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
		galleryIntent.setType("image/*");
		
		Intent chooser = Intent.createChooser(galleryIntent, "Open with");
		chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

		startActivityForResult(chooser, TAKE_PICTURE);
		
		
	}

	@Override
	protected void open(Object message) {
		Intent it = new Intent(this, ReceivePicsActivity.class);
		it.putExtra("image", toBitmap((byte[])message));
		startActivity(it);
	}
}
