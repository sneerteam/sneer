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

import sneer.main.R;

import static sneer.android.ui.SneerActivity.findView;

class ConversationListAdapter extends ArrayAdapter<ConversationListModel.Item> {

	private final Activity activity;

	public ConversationListAdapter(Activity activity) {
		super(activity, R.layout.list_item_main);
		this.activity = activity;
	}

	public void update(List<ConversationListModel.Item> summaries) {
		clear();
		addAll(summaries);
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView == null
			? createView(parent)
			: convertView;

		bindConversation(position, view);
		return view;
	}

	private View createView(ViewGroup parent) {
		View view = inflateConversationView(parent);
		tagNewWidgetOnto(view);
		return view;
	}

	private View inflateConversationView(ViewGroup parent) {
		LayoutInflater inflater = activity.getLayoutInflater();
		return inflater.inflate(R.layout.list_item_main, parent, false);
	}

	private void tagNewWidgetOnto(View view) {
		ConversationWidget widget = new ConversationWidget();
		widget.conversationParty = findView(view, R.id.conversationParty);
		widget.conversationSummary = findView(view, R.id.conversationSummary);
		widget.conversationDate = findView(view, R.id.conversationDate);
		widget.conversationPicture = findView(view, R.id.conversationPicture);
		widget.conversationSummary.getPaint().setShader(textShader);
		widget.conversationUnread = findView(view, R.id.conversationUnread);
		view.setTag(widget);
	}

	private void bindConversation(int position, View view) {
		ConversationListModel.Item conversation = getItem(position);
		((ConversationWidget)view.getTag()).bind(conversation);
	}

	private class ConversationWidget {
		TextView conversationParty;
		TextView conversationSummary;
		TextView conversationDate;
		TextView conversationUnread;
		ImageView conversationPicture;

		public void bind(ConversationListModel.Item conversation) {
			conversationParty.setText(conversation.party);
			conversationSummary.setText(conversation.textPreview);
			conversationDate.setText(conversation.date);
			if (conversation.unread.isEmpty()) {
				conversationUnread.setVisibility(View.GONE);
			} else {
				conversationUnread.setVisibility(View.VISIBLE);
				conversationUnread.setText(conversation.unread);
			}
		}
	}

	private final Shader textShader = new LinearGradient(200, 0, 650, 0,
			new int[] {Color.DKGRAY, Color.LTGRAY},
			new float[] {0, 1}, Shader.TileMode.CLAMP);

}
