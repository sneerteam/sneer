package diegomendes.sendpics;

import sneer.android.ui.MessageActivity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.widget.ImageView;

public class ViewSendPicsActivity extends MessageActivity {

	private ImageView imageView;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_receive_pics);

		imageView = (ImageView)findViewById(R.id.picture_received);

		Bitmap bmp = (Bitmap)toBitmap(messageJpegImage());
		imageView.setImageBitmap(bmp);

		addImageToGallery(getApplicationContext().getFilesDir().getAbsolutePath(), getApplicationContext(), bmp);
	}
	
	
	public void addImageToGallery(final String filePath, final Context context, Bitmap yourBitmap) {
	    ContentValues values = new ContentValues();
	    
	    values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
	    values.put(Images.Media.MIME_TYPE, "image/jpeg");
	    values.put(MediaStore.MediaColumns.DATA, filePath);

	    context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
	    
	    MediaStore.Images.Media.insertImage(context.getContentResolver(), yourBitmap, "Sendpics" , "");
	    
	    imageView.setImageBitmap(yourBitmap);
	}
	
}
