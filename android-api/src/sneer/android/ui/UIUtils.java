package sneer.android.ui;

import rx.*;
import rx.android.schedulers.*;
import rx.functions.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;

public class UIUtils {

	public static Subscription plug(final TextView textView, Observable<?> observable) {
		return observable.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Object>() { @Override public void call(Object obj) {
			textView.setText(obj.toString());
		}});
	}

	public static Subscription plug(final ImageView imageView, Observable<byte[]> observable) {
		return observable.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<byte[]>() { @Override public void call(byte[] obj) {
			imageView.setImageBitmap(BitmapFactory.decodeByteArray(obj, 0, obj.length));
		}});
	}
	
	public static Subscription plugHeaderTitle(final ContextMenu menu, Observable<?> observable) {
		return observable.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Object>() { @Override public void call(Object obj) {
			menu.setHeaderTitle(obj.toString());
		}});
	}
	
	@SuppressWarnings("unchecked")
	static public <V> V findView(View view, int id) {
		return (V)view.findViewById(id);
	}

	static public TextView findTextView(View view, int id) {
		return (TextView)view.findViewById(id);
	}

}
