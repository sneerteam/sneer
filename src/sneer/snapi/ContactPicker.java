package sneer.snapi;

import static sneer.snapi.SneerUtils.*;
import rx.*;
import rx.Observable.OnSubscribe;
import rx.android.schedulers.*;
import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;

public class ContactPicker {

	public static Observable<Contact> pickContact(final Context context) {
	    return Observable.create(new OnSubscribe<Contact>() {@Override public void call(final Subscriber<? super Contact> subscriber) {
	        Intent intent = new Intent("sneer.intent.action.PICK_CONTACT");
	        intent.putExtra("result", new ResultReceiver(null) {
	            @Override
	            protected void onReceiveResult(int resultCode, Bundle resultData) {
	                subscriber.onNext(new Contact(
	                        resultData.get("public_key").toString(),
	                        resultData.get("nickname").toString()));
	                subscriber.onCompleted();
	            }
	        });
	        try {
	        	context.startActivity(intent);
	        } catch (ActivityNotFoundException e) {
	            if (context instanceof Activity) {
	                showSneerInstallationMessageIfNecessary((Activity) context);
	            } else {
	            	Log.w(ContactPicker.class.getSimpleName(), "Can't start contact picker", e);
	            }
	        }
        }}).subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread());
	}
}
