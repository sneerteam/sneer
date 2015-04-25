package sneer.android.ui.flux;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import static sneer.android.ui.SneerActivity.onMainThread;

import rx.Observable;
import rx.functions.Action1;
import rx.subscriptions.SerialSubscription;
import sneer.main.R;

public class ConversationListActivity extends Activity {

	final SerialSubscription subscription = new SerialSubscription();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setUpConversationList();
	}

	@Override
	protected void onDestroy() {
		subscription.unsubscribe();
		super.onDestroy();
	}

	private void setUpConversationList() {

		final ConversationListAdapter adapter = new ConversationListAdapter(this);
		subscription.set(
			model().subscribe(new Action1<List<ConversationListModel.Item>>() { @Override public void call(List<ConversationListModel.Item> snapshot) {
				adapter.update(snapshot);
			}}));

		ListView conversationList = (ListView) findViewById(R.id.conversationList);
		conversationList.setAdapter(adapter);
		conversationList.setOnItemClickListener(new AdapterView.OnItemClickListener() { @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id_ignored) {
			ConversationListModel.Item conversation = adapter.getItem(position);
			onClicked(conversation);
		}});
	}

	private void onClicked(ConversationListModel.Item conversation) {
		toast(conversation.party);
	}

	private Observable<List<ConversationListModel.Item>> model() {
		return onMainThread(ConversationListModel.items());
	}

	private void toast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

}
