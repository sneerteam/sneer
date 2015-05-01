package sneer.android.flux2;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;
import sneer.android.ui.SneerActivity;
import sneer.main.R;

import static sneer.android.flux2.Components.component;

public class ConversationListActivity extends SneerActivity {

	private final ActionBus bus = component(ActionBus.class);
	private final Conversations convos = component(Conversations.class);
	private Subscription subscription;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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
			bus.action(new Conversations.Click(id));
		}});

		subscription = ui(convos.summaries()).subscribe(new Action1<List<Conversations.Summary>>() { @Override public void call(List<Conversations.Summary> summaries) {
			adapter.update(summaries);
		}});
	}

}
