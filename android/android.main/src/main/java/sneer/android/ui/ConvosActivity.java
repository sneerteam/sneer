package sneer.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;
import sneer.convos.Convos;
import sneer.convos.Summary;
import sneer.main.R;

import static sneer.android.SneerAndroidContainer.component;


public class ConvosActivity extends SneerActionBarActivity {

	private final Convos convos = component(Convos.class);
    private ConvosAdapter adapter;
	private Subscription subscription;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_convos);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);     // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                               // Setting toolbar as the ActionBar with setSupportActionBar() call

        setUpConversationList();
	}

    @Override
    protected void onStart() {
        super.onStart();
        subscription = ui(convos.summaries()).subscribe(new Action1<List<Summary>>() { @Override public void call(List<Summary> summaries) {
            adapter.update(summaries);
        }});
    }

    @Override
	protected void onStop() {
        if (subscription != null) subscription.unsubscribe();
		super.onStop();
	}

	private void setUpConversationList() {
        adapter = new ConvosAdapter(this);

        final ListView list = (ListView)findViewById(R.id.conversationList);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
				long convoId = adapter.getItem(position).convoId;
				ConvoActivity.open(ConvosActivity.this, convoId);
			}
		});

		registerForContextMenu(list);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Notifier.resume();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Notifier.pause();
	}

    public void onAddContactClicked(View view) {
        navigateTo(AddContactActivity.class);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_advanced) {
			navigateTo(SystemReportActivity.class);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v.getId() == R.id.conversationList) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.convos_menu_list, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.edit_contact:
				long convoId = adapter.getItem(info.position).convoId;
				String oldNickname = adapter.getItem(info.position).nickname;
				Intent intent = new Intent();
				intent.setClass(this, EditContactActivity.class);
				intent.putExtra("convoId", convoId);
				intent.putExtra("oldNickname", oldNickname);
				startActivity(intent);
				return true;
			case R.id.delete_contact:
				// not implemented yet
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

}
