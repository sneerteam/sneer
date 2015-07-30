package sneer.android.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import sneer.convos.SessionSummary;
import sneer.convos.Summary;
import sneer.main.R;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static sneer.android.ui.SneerActivity.findView;

class SessionListAdapter extends ArrayAdapter<SessionSummary> {

	private final LayoutInflater inflater;

	public SessionListAdapter(Activity activity) {
        super(activity, R.layout.list_item_session);
		inflater = activity.getLayoutInflater();
    }

	void update(List<SessionSummary> summaries) {
		clear();
		addAll(summaries);
		notifyDataSetChanged();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView == null
			? inflater.inflate(R.layout.list_item_session, parent, false)
			: convertView;

		updateSession(position, view);
		return view;
	}

	private void updateSession(int position, View view) {
		SessionSummary summary = getItem(position);

		// ImageView pic     = findView(view, R.id.sessionPicture);
		TextView  title   = findView(view, R.id.sessionTitle);
		TextView  date    = findView(view, R.id.sessionDate);
		TextView unread = findView(view, R.id.sessionUnread);

		title .setText(summary.title);
		date  .setText(summary.date);
		unread.setText(summary.unread);
		unread.setVisibility(summary.unread.isEmpty() ? GONE : VISIBLE);
	}

}
