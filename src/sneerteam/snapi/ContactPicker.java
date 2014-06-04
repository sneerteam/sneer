package sneerteam.snapi;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.android.schedulers.*;
import android.content.*;
import android.os.*;

public class ContactPicker {

	public static Observable<Contact> pickContact(final Context context) {
	    return Observable.create(new OnSubscribe<Contact>() {@Override public void call(final Subscriber<? super Contact> subscriber) {
	        Intent intent = new Intent("sneerteam.intent.action.PICK_CONTACT");
	        intent.putExtra("result", new ResultReceiver(null) {
	            @Override
	            protected void onReceiveResult(int resultCode, Bundle resultData) {
	                subscriber.onNext(new Contact(
	                        resultData.get("public_key").toString(),
	                        resultData.get("nickname").toString()));
	                subscriber.onCompleted();
	            }
	        });
	        context.startActivity(intent);
        }}).subscribeOn(AndroidSchedulers.mainThread()).observeOn(AndroidSchedulers.mainThread());
	}
}
