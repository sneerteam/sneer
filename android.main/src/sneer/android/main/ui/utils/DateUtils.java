package sneer.android.main.ui.utils;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.ocpsoft.prettytime.*;

import rx.*;
import rx.Observable;
import rx.android.schedulers.*;
import rx.functions.*;
import android.widget.*;

public class DateUtils {
	
	public static Subscription plugDate(final TextView textView, Observable<Long> dates) {
//		TODO Consider sharing a single timer for all interactions in the future if performance degrades
		return Observable.combineLatest(Observable.timer(0, 1, TimeUnit.MINUTES), dates, new Func2<Long, Long, Long>() { @Override public Long call(Long tick, Long date) {
			return date;
		}})
		.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() { @Override public void call(Long obj) {
			textView.setText(userFriendlyDateTime(obj.toString()));
		}});
	}
	
	public static String userFriendlyDateTime(CharSequence charSequence) {
		PrettyTime pt = new PrettyTime();
		long timestamp = Long.parseLong(charSequence.toString());
		return pt.format(new Date(timestamp));
	}

}


























