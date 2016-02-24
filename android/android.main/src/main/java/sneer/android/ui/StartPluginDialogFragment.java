package sneer.android.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import sneer.android.ipc.Plugin;
import sneer.android.ipc.Plugins;
import sneer.convos.Convo;
import sneer.main.R;

import static sneer.android.ipc.PluginActivities.SEARCH_SNEER_APPS_URL;
import static sneer.android.ipc.PluginActivities.start;

public class StartPluginDialogFragment extends DialogFragment {

    private SingleConvoProvider convoProvider;

    public StartPluginDialogFragment() { }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        String title = "Apps";
        switch (Plugins.appType) {
            case SESSION:
                title = "Session Apps";
                break;
            case TEXT:
                title = "Text Apps";
                break;
        }

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .create();

        final List<Plugin> plugins = Plugins.all(activity);
        final LayoutInflater inflater = activity.getLayoutInflater();
        ListView listView = (ListView) inflater.inflate(R.layout.plugins_list, null);

        listView.setAdapter(new ArrayAdapter<Plugin>(activity, R.layout.plugins_list_item, plugins) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView ret = (convertView != null)
                        ? (TextView) convertView
                        : (TextView) inflater.inflate(R.layout.plugins_list_item, null);

                Plugin plugin = plugins.get(position);
                ret.setText(plugin.caption);

                plugin.icon.setBounds(0, 0, 96, 96);    // TODO Not a good solution
                ret.setCompoundDrawables(plugin.icon, null, null, null);

                return ret;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            dialog.dismiss();
            start(activity, plugins.get(position), convoProvider.getConvo().id);
        }});

        dialog.setView(listView, 0, 0, 0, 0);

        dialog.setButton(Dialog.BUTTON_NEUTRAL, "Search for Apps", new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which != Dialog.BUTTON_NEUTRAL) return;
                try {
                    Intent urlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(SEARCH_SNEER_APPS_URL));
                    urlIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(urlIntent);
                } catch (ActivityNotFoundException e) {
                    // TODO Ignore by now
                }
            }
        });

        return dialog;
    }


    @Override
    public void onAttach(Activity activity) {
        if (!(activity instanceof SingleConvoProvider))
            throw new ClassCastException(activity.toString()
                    + " must implement " + SingleConvoProvider.class.getName());

        convoProvider = (SingleConvoProvider) activity;

        super.onAttach(activity);
    }


    public interface SingleConvoProvider {
        Convo getConvo();
    }

}
