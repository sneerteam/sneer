package sneer.android.ui.flux;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import sneer.android.ui.flux.ConversationListModel;
import sneer.main.R;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static sneer.android.ui.SneerActivity.findView;

class ConversationListAdapter extends ArrayAdapter<ConversationListModel.Item> {

	private final Activity activity;

	ConversationListAdapter(Activity activity) {
		super(activity, R.layout.list_item_main);
		this.activity = activity;
	}

	void update(List<ConversationListModel.Item> summaries) {
		clear();
		addAll(summaries);
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView == null
			? inflateConversationView(parent)
			: convertView;

		setConversation(position, view);
		return view;
	}

	private View inflateConversationView(ViewGroup parent) {
		LayoutInflater inflater = activity.getLayoutInflater();
		return inflater.inflate(R.layout.list_item_main, parent, false);
	}

	private void setConversation(int position, View view) {
		ConversationListModel.Item conversation = getItem(position);

		ImageView pic     = findView(view, R.id.conversationPicture);
		TextView  party   = findView(view, R.id.conversationParty);
		TextView  date    = findView(view, R.id.conversationDate);
		TextView  unread  = findView(view, R.id.conversationUnread);
		TextView  summary = findView(view, R.id.conversationSummary);
		summary.getPaint().setShader(textShader);

		party  .setText(conversation.party);
		summary.setText(conversation.textPreview);
		date   .setText(conversation.date);
		unread .setText(conversation.unread);
		unread.setVisibility(conversation.unread.isEmpty() ? GONE : VISIBLE);
	}

	private final Shader textShader = new LinearGradient(200, 0, 650, 0,
			new int[] {Color.DKGRAY, Color.LTGRAY},
			new float[] {0, 1}, Shader.TileMode.CLAMP);

}
