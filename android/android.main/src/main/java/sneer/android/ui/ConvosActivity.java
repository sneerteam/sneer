package sneer.android.ui;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
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

}
