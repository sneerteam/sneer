package sneer.android.ui;

import rx.*;
import rx.android.schedulers.*;
import rx.functions.*;
import android.view.*;
import android.widget.*;

public class UIUtils {

	public static void subscribeTextView(final TextView textView, Observable<?> observable) {
		observable.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Object>() { @Override public void call(Object obj) {
			textView.setText(obj.toString());
		}});
	}

	public static void subscribeMenuHeader(final ContextMenu menu, Observable<?> observable) {
		observable.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Object>() { @Override public void call(Object obj) {
			menu.setHeaderTitle(obj.toString());
		}});
	}

	@SuppressWarnings("unchecked")
	static public <V> V findView(View view, int id) {
		return (V)view.findViewById(id);
	}

	@SuppressWarnings("unchecked")
	static public TextView findText(View view, int id) {
		return (TextView)view.findViewById(id);
	}

}
