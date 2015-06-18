package location;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;

import sneer.android.PartnerSession;

public class MapDownloader extends AsyncTask<String, Void, Bitmap> {

	ImageView bmImage;
	private final Activity activity;
	private final PartnerSession session;
	int width, height;
	private String url;

	public MapDownloader(ImageView bmImage, int width, int height, Activity activity, PartnerSession session) {
		this.width = width;
        this.height = height;
        this.bmImage = bmImage;
		this.activity = activity;
		this.session = session;
	}

	protected Bitmap doInBackground(String... urls) {
		url = urls[0];

		Log.d("FELIPETESTE", "urls->" + urls);
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
		activity.setProgressBarIndeterminateVisibility(false);
		activity.setProgressBarVisibility(false);
	}

}
