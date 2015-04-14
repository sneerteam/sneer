package sneer.android.ipc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;


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
        AlertDialog dialog = createPluginsDialog();
        dialog.show();
    }

    private AlertDialog createPluginsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Launch SneerApp");

        final List<Plugin> plugins = InstalledPlugins.all(this);

        final LayoutInflater inflater = getLayoutInflater();
        ListView listView = (ListView) inflater.inflate(R.layout.plugins_list, null);

        ArrayAdapter<Plugin> adapter = new ArrayAdapter<Plugin>(this, R.layout.plugins_list_item, plugins) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView;
                if (convertView != null)
                    textView = (TextView) convertView;
                else
                    textView = (TextView) inflater.inflate(R.layout.plugins_list_item, null);

                Plugin plugin = plugins.get(position);
                textView.setText(plugin.caption);

                plugin.icon.setBounds(0, 0, 84, 84);    // TODO Not a good solution
                textView.setCompoundDrawables(plugin.icon, null, null, null);

                return textView;
            }
        };
        listView.setAdapter(adapter);

        AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launch(IpcServer.this, plugins.get(position));
            }
        };
        listView.setOnItemClickListener(clickListener);

        AlertDialog dialog = builder.create();
        dialog.setView(listView, 0, 0, 0, 0);

        return dialog;
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
