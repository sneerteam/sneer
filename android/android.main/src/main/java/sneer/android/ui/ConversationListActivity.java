package sneer.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


import java.util.List;

import rx.Subscription;
import rx.functions.Action1;
import sneer.commons.ActionBus;
import sneer.conversations.ConversationList;
import sneer.main.R;

import static sneer.commons.Container.singleton;


public class ConversationListActivity extends SneerActivity {

	private final ActionBus bus = singleton(ActionBus.class);
	private final ConversationList convos = singleton(ConversationList.class);
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
			bus.action(new ConversationList.Open(id));
		}});

		subscription = ui(convos.summaries()).subscribe(new Action1<List<ConversationList.Summary>>() { @Override public void call(List<ConversationList.Summary> summaries) {
			adapter.update(summaries);
		}});
	}

}
