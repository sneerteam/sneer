package location;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;

public class MapDownloader extends AsyncTask<String, Void, Bitmap> {

	ImageView bmImage;
    int width, height;

	public MapDownloader(ImageView bmImage, int width, int height) {
		this.width = width;
        this.height = height;
        this.bmImage = bmImage;
	}

	protected Bitmap doInBackground(String... urls) {
		String url = urls[0];
		Bitmap mIcon = null;
		try {
			InputStream in = new java.net.URL(url).openStream();
			mIcon = BitmapFactory.decodeStream(in);
		} catch (Exception e) {
			Log.e("Error", e.getMessage());
		}
		return mIcon;
	}

	protected void onPostExecute(Bitmap result) {
        Bitmap newbitMap = Bitmap.createScaledBitmap(result, width, height, true);
        bmImage.setImageBitmap(newbitMap);
	}

}
