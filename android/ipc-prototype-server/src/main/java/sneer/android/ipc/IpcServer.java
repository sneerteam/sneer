package sneer.android.ipc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class IpcServer extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ipc_server);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_ipc_server, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_settings) return true;

		return super.onOptionsItemSelected(item);
	}


    public void launchInstalledPlugins(View view) {
		for (Plugin p : InstalledPlugins.all(this))
			launch(view.getContext(), p);
    }

	private void launch(Context context, Plugin p) {
		Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(p.packageName, p.activityName);
        intent.putExtra(SendMessage.SEND_MESSAGE, sendMessageIntent("0123456789ABCDEF"));
        context.startActivity(intent);
	}


	private Intent sendMessageIntent(String convoToken) {
		return new Intent()
				.setClassName("sneer.android.ipc", "sneer.android.ipc.SendMessage")
				.putExtra(SendMessage.CONVERSATION_TOKEN, convoToken);

	}

}
