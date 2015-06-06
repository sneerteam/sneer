package sneer.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;
import sneer.convos.Convos;
import sneer.main.R;

import static sneer.android.SneerAndroidContainer.component;
import static sneer.convos.Convos.Summary;


public class ConversationListActivity extends SneerActionBarActivity {

	private final Convos convos = component(Convos.class);
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
    		String nick = adapter.getItem(position).nickname;
            openConversation(nick);
		}});

		subscription = ui(convos.summaries()).subscribe(new Action1<List<Summary>>() { @Override public void call(List<Summary> summaries) {
			adapter.update(summaries);
		}});
	}

    public void onAddContactClicked(View view) {
        navigateTo(AddContactActivity.class);
    }

    private void openConversation(String nick) {
        // sendMessageIfPresent(conversation);
        Intent intent = new Intent();
        intent.setClass(this, ConversationActivity.class);
        intent.putExtra("nick", nick);
        startActivity(intent);
    }

}
