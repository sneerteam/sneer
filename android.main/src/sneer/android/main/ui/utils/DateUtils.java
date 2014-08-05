package sneer.android.main.ui.utils;

import java.util.*;

import org.ocpsoft.prettytime.*;

import rx.Observable;
import rx.android.schedulers.*;
import rx.functions.*;
import android.widget.*;

public class DateUtils {
	
	public static void plugDate(final TextView textView, Observable<Long> observable) {
		observable.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() { @Override public void call(Long obj) {
			textView.setText(userFriendlyDateTime(obj.toString()));
		}});
	}
	
	public static String userFriendlyDateTime(CharSequence charSequence) {
		PrettyTime pt = new PrettyTime();
		long timestamp = Long.parseLong(charSequence.toString());
		return pt.format(new Date(timestamp));
	}

}
