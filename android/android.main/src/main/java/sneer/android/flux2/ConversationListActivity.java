package sneer.android.flux2;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import sneer.commons.Consumer;
import sneer.main.R;

public class ConversationListActivity extends Activity {

	private final Dispatcher dispatcher = Dispatcher.Factory.produceFor(getApplicationContext());
	private final Conversations convos = dispatcher.produce(Conversations.class);
	private final Lease lease = new Lease();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setUpConversationList();
	}

	@Override
	protected void onDestroy() {
		lease.dispose();
		super.onDestroy();
	}

	private void setUpConversationList() {
		final ConversationListAdapter adapter = new ConversationListAdapter(this);

		final ListView list = (ListView)findViewById(R.id.conversationList);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
			long id = adapter.getItem(position).id;
			dispatcher.dispatch(new Conversations.Click(id));
		}});

		convos.summaries().addConsumer(lease, new Consumer<List<Conversations.Summary>>() { @Override public void consume(final List<Conversations.Summary> summaries) {
			//TODO Sample 1 second.
			list.post(new Runnable() { @Override public void run() {
				adapter.update(summaries);
			}});
		}});
	}

}
