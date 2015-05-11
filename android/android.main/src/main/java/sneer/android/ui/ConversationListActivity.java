package sneer.android.ui;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;
import sneer.commons.ActionBus;
import sneer.conversations.ConversationList;
import sneer.main.R;

import static sneer.android.SneerAndroidContainer.component;


public class ConversationListActivity extends SneerActionBarActivity {

	private final ActionBus bus = component(ActionBus.class);
	private final ConversationList convos = component(ConversationList.class);
	private Subscription subscription;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_conversation_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);     // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                               // Setting toolbar as the ActionBar with setSupportActionBar() call

        setUpConversationList();
	}

	@Override
	protected void onStop() {
		if (subscription != null) subscription.unsubscribe();
		super.onStop();
	}

	private void setUpConversationList() {
		final ConversationListAdapter adapter = new ConversationListAdapter(this);

		final ListView list = (ListView)findViewById(R.id.conversationList);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
			long id = adapter.getItem(position).id;
			bus.action(new ConversationList.Open(id));
		}});

		subscription = ui(convos.summaries()).subscribe(new Action1<List<ConversationList.Summary>>() { @Override public void call(List<ConversationList.Summary> summaries) {
			adapter.update(summaries);
		}});
	}

}
