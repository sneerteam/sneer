package diegomendes.sendpics;

import java.util.HashMap;

import sneer.android.ui.MessageActivity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.widget.ImageView;

public class ViewSendPicsActivity extends MessageActivity {

	private ImageView image;
	private byte[] ret;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_receive_pics);

		image = (ImageView)findViewById(R.id.picture_received);

		extractImage(messagePayload());

		addImageToGallery(getApplicationContext().getFilesDir().getAbsolutePath(), getApplicationContext(), toBitmap(ret));
	}
	
	
	public void addImageToGallery(final String filePath, final Context context, Bitmap yourBitmap) {
	    ContentValues values = new ContentValues();
	    
	    values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
	    values.put(Images.Media.MIME_TYPE, "image/jpeg");
	    values.put(MediaStore.MediaColumns.DATA, filePath);

	    context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
	    
	    MediaStore.Images.Media.insertImage(context.getContentResolver(), yourBitmap, "Sendpics" , "");
	    
	    image.setImageBitmap(yourBitmap);
	}
	
	private void extractImage(Object message) {
		@SuppressWarnings("unchecked")
		HashMap<String, Object> map = (HashMap<String, Object>) message;
			
		ret = (byte[]) map.get("pics");
		image.setImageBitmap((Bitmap) toBitmap(ret));
	}

}
