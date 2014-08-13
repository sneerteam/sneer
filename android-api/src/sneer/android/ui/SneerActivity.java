package sneer.android.ui;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.ocpsoft.prettytime.*;

import rx.*;
import rx.Observable;
import rx.android.schedulers.*;
import rx.functions.*;
import android.app.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;

public class SneerActivity extends Activity {

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

	
	public static Subscription plugDate(final TextView textView, Observable<Long> dates) {
//		TODO Consider sharing a single timer for all interactions in the future if performance degrades
		return Observable.combineLatest(Observable.timer(0, 1, TimeUnit.MINUTES), dates, new Func2<Long, Long, Long>() { @Override public Long call(Long tick, Long date) {
			return date;
		}})
		.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() { @Override public void call(Long timestamp) {
			textView.setText(userFriendly(timestamp));
		}});
	}
	
	
	public static String userFriendly(Long timestamp) {
		return new PrettyTime().format(new Date(timestamp));
	}
	
	
	@SuppressWarnings("unchecked")
	static public <V> V findView(View view, int id) {
		return (V)view.findViewById(id);
	}

	
	static public TextView findTextView(View view, int id) {
		return (TextView)view.findViewById(id);
	}

}
