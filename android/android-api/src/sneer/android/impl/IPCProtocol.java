package sneer.android.impl;

import android.app.Activity;
import android.os.Bundle;
import android.os.ResultReceiver;

public class IPCProtocol {

	//Message
	public static final String LABEL = "label";
	public static final String PAYLOAD = "payload";
    public static final String JPEG_IMAGE = "jpeg-image";
	public static final String RESULT_RECEIVER = "result";

	//Session
	public static final String PARTNER_NAME = "partnerName";
	public static final String OWN = "own";
	public static final String REPLAY_FINISHED = "replayFinished";
	public static final String ERROR = "error";
	public static final String UNSUBSCRIBE = "unsubscribe";

}
