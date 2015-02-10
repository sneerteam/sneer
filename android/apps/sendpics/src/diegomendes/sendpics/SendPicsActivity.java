package diegomendes.sendpics;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import sneer.android.ui.MessageActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static android.widget.Toast.LENGTH_LONG;

public class SendPicsActivity extends MessageActivity {

	private static final int TAKE_PICTURE = 1;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        composeMessage();
    }
    
    
    private void composeMessage() {
		Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
		galleryIntent.setType("image/*");
		
		Intent chooser = Intent.createChooser(galleryIntent, "Open with");
		chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
	
		startActivityForResult(chooser, TAKE_PICTURE);
	}


	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)  {
    	if (requestCode != TAKE_PICTURE || resultCode != RESULT_OK || intent == null) {
    		finish();
    		return;
    	}
		
        Bitmap bitmap;
		try {
			bitmap = loadBitmap(intent);
		} catch (IOException e) {
			toast("Unable to read file");
			return;
		}

		byte[] imageBytes = scaledDownTo(bitmap, 40 * 1024);
		send("pic", null, imageBytes);
		finish();
    }

    private void toast(String text) {
        Toast.makeText(this, text, LENGTH_LONG).show();
    }


    protected Bitmap loadBitmap(Intent intent) throws IOException {
        final Bundle extras = intent.getExtras();
        if (extras != null && extras.get("data") != null)
            return (Bitmap) extras.get("data");

        Uri uri = intent.getData();

        return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
    }


    public static byte[] scaledDownTo(Bitmap original, int maximumLength) {
        int side = Math.min(original.getHeight(), original.getWidth());
        Bitmap reduced = original;
        while (true) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            reduced.compress(Bitmap.CompressFormat.JPEG, 100, out);
            final byte[] bytes = out.toByteArray();
            if (bytes.length <= maximumLength)
                return bytes;
            side = (int) (side * 0.9f);
            reduced = ThumbnailUtils.extractThumbnail(original, side, side);
        }
    }

}
