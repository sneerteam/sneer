package sneer.android.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;
import sneer.Conversation;
import sneer.android.R;

import static sneer.android.SneerAndroidSingleton.sneer;
import static sneer.android.ui.SneerActivity.*;

public class MainAdapter extends ArrayAdapter<Conversation> {

	private final Activity activity;
	private final CompositeSubscription subscriptions;
	private final Shader textShader = new LinearGradient(200, 0, 650, 0,
			new int[] {Color.DKGRAY, Color.LTGRAY},
			new float[] {0, 1}, TileMode.CLAMP);

	public MainAdapter(Activity activity) {
        super(activity, R.layout.list_item_main);
        this.activity = activity;
		subscriptions = new CompositeSubscription();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

		Conversation conversation = getItem(position);
		if (convertView == null) {
			View view = inflateConversationView(parent);

			final ConversationWidget widget = conversationWidgetFor(view);
			widget.bind(conversation);

			subscriptions.add(widget.subscription);
			return view;
        } else {
			ConversationWidget existing = (ConversationWidget) convertView.getTag();
			if (existing.conversation != conversation) {
				existing.recycle();
				existing.bind(conversation);
			}
			return convertView;
		}

    }

	private View inflateConversationView(ViewGroup parent) {
		LayoutInflater inflater = activity.getLayoutInflater();
		return inflater.inflate(R.layout.list_item_main, parent, false);
	}

	private ConversationWidget conversationWidgetFor(View view) {
		final ConversationWidget widget = new ConversationWidget();
		widget.conversationParty = findView(view, R.id.conversationParty);
		widget.conversationSummary = findView(view, R.id.conversationSummary);
		widget.conversationDate = findView(view, R.id.conversationDate);
		widget.conversationPicture = findView(view, R.id.conversationPicture);
		widget.conversationSummary.getPaint().setShader(textShader);
		widget.conversationUnread = findView(view, R.id.conversationUnread);
		view.setTag(widget);
		return widget;
	}

	}


	class ConversationWidget {
		Conversation conversation;
		TextView conversationParty;
		TextView conversationSummary;
		TextView conversationDate;
		TextView conversationUnread;
		ImageView conversationPicture;
		final SerialSubscription subscription = new SerialSubscription();

		void recycle() {
			conversation = null;
			conversationParty.setText("");
			conversationSummary.setText("");
			conversationDate.setText("");
			hide(conversationUnread);
		}

		void bind(Conversation conversation) {
			if (this.conversation != null) throw new IllegalStateException();
			this.conversation = conversation;
			subscription.set(
				Subscriptions.from(
					plug(conversationParty, conversation.party().name()),
					plug(conversationSummary, conversation.mostRecentMessageContent()),
					plug(conversationPicture, sneer().profileFor(conversation.party()).selfie()),
					plugUnreadMessage(conversationUnread, conversation.unreadMessageCount()),
					plug(conversationDate, prettyTime(conversation.mostRecentMessageTimestamp()))
				));
		}
	}


	public static Subscription plugUnreadMessage(final TextView textView, Observable<Long> observable) {
		return deferUI(observable).subscribe(new Action1<Long>() { @Override public void call(Long obj) {
			if (obj == 0)
				hide(textView);
			else
				show(textView);
			textView.setText(obj.toString());
		}});
	}

	private static void show(View textView) {
		textView.setVisibility(View.VISIBLE);
	}

	private static void hide(View textView) {
		textView.setVisibility(View.GONE);
	}

	public void dispose() {
		subscriptions.unsubscribe();
	}

}
