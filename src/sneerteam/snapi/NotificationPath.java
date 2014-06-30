package sneerteam.snapi;

import rx.*;

public interface NotificationPath {
	
	void pub(String receiverPuk, CharSequence contentText, Object payload);
	
	Observable<CloudNotification> notifications();
	
}
