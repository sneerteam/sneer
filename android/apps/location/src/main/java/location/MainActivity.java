package location;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import sneer.android.impl.IPCProtocol;


public class MainActivity extends Activity {

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final CharSequence options[] = new CharSequence[] {"Send current location", "Follow me for one hour"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose your action");
		builder.setItems(options, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent();
				intent.putExtra("SEND_MESSAGE", getIntent().<Intent>getParcelableExtra(IPCProtocol.SEND_MESSAGE));
				intent.putExtra("JOIN_SESSION", getIntent().<Intent>getParcelableExtra(IPCProtocol.JOIN_SESSION));
				intent.setClass(MainActivity.this, which == 0 ? LocationActivity.class : FollowMeActivity.class);
				startActivity(intent);
			}
		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() { @Override public void onCancel(DialogInterface dialog) {
			finish();
		}});
		builder.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

}



