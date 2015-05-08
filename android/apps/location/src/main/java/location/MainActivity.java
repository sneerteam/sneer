package location;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity {

    private LocationManager locationManager;
    private Location latestLocation;
    private TextView textAccuracy;
    private Button sendButton;


    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        final CharSequence colors[] = new CharSequence[] {"Send current location", "Follow me for one hour"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose your action");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                navigateTo(which == 0 ? LocationActivity.class : FollowMeActivity.class);
                progressDialog("Doing stuff");
            }
        });
        builder.show();
    }

    public void navigateTo(Class<?> class1) {
        startActivity(new Intent().setClass(this, class1));
    }

    private void progressDialog(String message) {
        ProgressDialog ret = ProgressDialog.show(this, null, message);
        ret.setIndeterminate(true);
        ret.setCancelable(true); ret.setOnCancelListener(new DialogInterface.OnCancelListener() { @Override public void onCancel(DialogInterface dialog) {
            finish();
        }});
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



