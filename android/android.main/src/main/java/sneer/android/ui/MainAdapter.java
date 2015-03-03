package sneer.android.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;
import sneer.Conversation;
import sneer.android.R;
import sneer.rx.ObservedSubject;

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
			existing.bind(conversation);
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

	private ObservedConversation observe(final Conversation c) {
		ObservedConversation existing = observedConversations.get(c);
		if (existing != null)
			return existing;

		final ObservedConversation oc = new ObservedConversation();
		new AsyncTask<Void, Void, Void>() { @Override protected Void doInBackground(Void... voids) {
			subscriptions.add(oc.subscribe(c));
			return null;
		}}.execute();

		observedConversations.put(c, oc);
		return oc;
	}

	private final Map<Conversation, ObservedConversation> observedConversations
			= new HashMap<Conversation, ObservedConversation>();

	class ConversationWidget {
		Conversation conversation;
		TextView conversationParty;
		TextView conversationSummary;
		TextView conversationDate;
		TextView conversationUnread;
		ImageView conversationPicture;
		final SerialSubscription subscription = new SerialSubscription();

		void bind(Conversation conversation) {
			if (this.conversation == conversation) return;
			this.conversation = conversation;
			ObservedConversation oc = observe(conversation);
			subscription.set(
				Subscriptions.from(
					bind(conversationParty, oc.party),
					bind(conversationSummary, oc.summary),
					bind(conversationDate, oc.timestamp),
					bindPicture(oc.picture),
					bindUnread(oc.unread)
				));
		}

		private Subscription bindPicture(ObservedSubject<Bitmap> picture) {
			setPicture(picture.current());
			return deferUI(picture.observable()).subscribe(new Action1<Bitmap>() { @Override public void call(Bitmap bitmap) {
				setPicture(bitmap);
			}});
		}

		private void setPicture(Bitmap bitmap) {
			if (bitmap == null) {
				hide(conversationPicture);
			} else {
				conversationPicture.setImageBitmap(bitmap);
				show(conversationPicture);
			}
		}

		private Subscription bind(TextView view, ObservedSubject<String> subject) {
			view.setText(subject.current());
			return plug(view, subject.observable());
		}

		private Subscription bindUnread(ObservedSubject<Long> subject) {
			setMessageUnread(subject.current());
			return deferUI(subject.observable()).subscribe(new Action1<Long>() {@Override public void call(Long unread) {
				setMessageUnread(unread);
			}});
		}

		private void setMessageUnread(Long unread) {
			if (unread == 0)
				hide(conversationUnread);
			else
				show(conversationUnread);
			conversationUnread.setText(unread.toString());
		}
	}

	private Observable<byte[]> pictureFor(Conversation conversation) {
		return sneer().profileFor(conversation.party()).selfie();
	}

	class ObservedConversation {
		public final ObservedSubject<String> party = ObservedSubject.create("");
		public final ObservedSubject<String> summary = ObservedSubject.create("");
		public final ObservedSubject<Bitmap> picture = ObservedSubject.create(null);
		public final ObservedSubject<Long> unread = ObservedSubject.create(0L);
		public final ObservedSubject<String> timestamp = ObservedSubject.create("");

		public Subscription subscribe(Conversation conversation) {
			return Subscriptions.from(
					conversation.party().name().subscribe(party),
					conversation.mostRecentMessageContent().subscribe(summary),
					pictureFor(conversation).map(TO_BITMAP).subscribe(picture),
					conversation.unreadMessageCount().subscribe(unread),
					prettyTime(conversation.mostRecentMessageTimestamp()).subscribe(timestamp)
			);
		}
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
