package sneer.android.impl;

import android.content.Intent;
import android.os.Bundle;

public class Utils {

    public static <T> T getExtra(Intent intent, String extra) {
        Bundle extras = intent.getExtras();
        return extras == null ? null : (T)extras.get(extra);
    }


}
