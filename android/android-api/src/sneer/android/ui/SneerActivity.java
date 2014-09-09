package sneer.android.ui;

import static android.widget.Toast.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.ocpsoft.prettytime.*;

import rx.*;
import rx.Observable;
import rx.android.schedulers.*;
import rx.functions.*;
import rx.schedulers.*;
import sneer.commons.exceptions.*;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;

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

	
	protected void toast(Object text) { toast(text, LENGTH_SHORT); }
	protected void toast(FriendlyException e) { toast(e.getMessage(), LENGTH_LONG); }
	protected void toast(Object text, int length) { Toast.makeText(this, text.toString(), length).show(); }

	
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
}

