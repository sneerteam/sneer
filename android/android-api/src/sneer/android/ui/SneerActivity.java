package sneer.android.ui;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.ocpsoft.prettytime.PrettyTime;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import sneer.commons.exceptions.FriendlyException;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SneerActivity extends Activity {

	public static final Func1<byte[], Bitmap> TO_BITMAP = new Func1<byte[], Bitmap>() { @Override public Bitmap call(byte[] bytes) {
		return toBitmap(bytes);
	}};


	public static Observable<Long> EVERY_MINUTE = Observable.timer(0, 1, TimeUnit.MINUTES).share();


	public static Subscription plug(final TextView textView, Observable<?> observable) {
		return deferUI(observable).subscribe(new Action1<Object>() { @Override public void call(Object obj) {
			textView.setText(obj.toString());
		}});
	}
	
	
	public static Subscription plug(final EditText editText, Observable<?> observable) {
		return deferUI(observable).subscribe(new Action1<Object>() { @Override public void call(Object obj) {
			editText.setText(obj.toString());
		}});
	}

	
	public static Subscription plug(final ImageView imageView, Observable<byte[]> observable) {
		return deferUI(observable.map(TO_BITMAP)).subscribe(new Action1<Bitmap>() { @Override public void call(Bitmap bitmap) {
			imageView.setImageBitmap(bitmap);
		}});
	}

	
	protected Subscription plugActionBarIcon(ActionBar actionBar, Observable<byte[]> observable) {
		return plugActionBarIcon(actionBar, observable, getResources());
	}
	public static Subscription plugActionBarIcon(final ActionBar actionBar, Observable<byte[]> observable, Resources resources) {
		return deferUI(observable.map(toDrawable(resources))).subscribe(new Action1<Drawable>() { @Override public void call(Drawable drawable) {
			actionBar.setIcon(drawable);
		}});
	}
	public static Subscription plugActionBarTitle(final ActionBar actionBar, Observable<?> observable) {
		return deferUI(observable).subscribe(new Action1<Object>() { @Override public void call(Object obj) {
			actionBar.setTitle(obj.toString());
		}});
	}
	
	
	public static Subscription plugHeaderTitle(final ContextMenu menu, Observable<?> observable) {
		return deferUI(observable).subscribe(new Action1<Object>() { @Override public void call(Object obj) {
			menu.setHeaderTitle(obj.toString());
		}});
	}
	
	
	public static Subscription plugDate(final TextView textView, Observable<Long> date) {
		return plug(textView, Observable.combineLatest(EVERY_MINUTE, date, new Func2<Long, Long, String>() { @Override public String call(Long tickIgnored, Long date) {
			return prettyTime(date);
		}}));
	}
	
	
	public static String prettyTime(Long timestamp) {
		return new PrettyTime().format(new Date(timestamp));
	}
	
	
	@SuppressWarnings("unchecked")
	static public <V> V findView(View view, int id) {
		return (V)view.findViewById(id);
	}
	
	
	protected Button button(int id) {
		return (Button)findViewById(id);
	}
	
	
	protected TextView textView(int id) {
		return (TextView)findViewById(id);
	}

	
	static public TextView findTextView(View view, int id) {
		return (TextView)view.findViewById(id);
	}

	
	static public ImageView findImageView(View view, int id) {
		return (ImageView)view.findViewById(id);
	}


	protected void toast(Object text) { toast(text, LENGTH_SHORT); }
	protected void toast(FriendlyException e) { toast(e.getMessage(), LENGTH_LONG); }
	protected void toast(final Object text, final int length) {
		this.runOnUiThread(new Runnable() {  @Override public void run() {
			Toast.makeText(SneerActivity.this, text.toString(), length).show(); 
		} });
	}

	
	@SuppressWarnings("unchecked")
	protected <T> T getExtra(String extra) {
		Bundle extras = getIntent().getExtras();
		return extras == null ? null : (T)extras.get(extra);
	}
	
	
	public static <T> Observable<T> deferUI(Observable<T> observable) {
		return observable
				.subscribeOn(Schedulers.computation())
				.observeOn(AndroidSchedulers.mainThread());
	}
	
	public static Bitmap toBitmap(byte[] bytes) {
		return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
	}
	
	
	public static Func1<byte[], Drawable> toDrawable(final Resources resources) {
		return new Func1<byte[], Drawable>() { @Override public Drawable call(byte[] bytes) {
			return toDrawable(bytes, resources);
		}};
	}
	public static Drawable toDrawable(byte[] bytes, Resources resources) {
		return new BitmapDrawable(resources, toBitmap(bytes));
	}
	
	
	protected Bitmap loadBitmap(Intent intent) throws FriendlyException {
		final Bundle extras = intent.getExtras();
		if (extras != null && extras.get("data") != null)
			return (Bitmap)extras.get("data");
		
		Uri uri = intent.getData();
		if (uri == null)
			throw new FriendlyException("No image found.");
	
		try {
			return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
		} catch (FileNotFoundException e) {
			throw new FriendlyException("Unable to load image: " + uri);
		}
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
	
	
	public void navigateTo(Class<?> class1) {
		startActivity(new Intent().setClass(this, class1));
	}

	
	protected void alert(String title, CharSequence[] items, DialogInterface.OnClickListener onClickListener) {
		new AlertDialog.Builder(this)
			.setTitle(title)
			.setItems(items, onClickListener)
			.show()
			.setOnCancelListener(new OnCancelListener() {  @Override public void onCancel(DialogInterface dialog) {
				finish();
			}});;
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
	
	
	public static void log(Activity activity, Object obj) {
		Log.d(activity.getClass().getSimpleName(), obj.toString());
	}
	
}

