package sneer.android.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.SpannableString;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import sneer.android.ui.drawable.TriangleLeftDrawable;
import sneer.android.ui.drawable.TriangleRightDrawable;
import sneer.convos.ChatMessage;
import sneer.main.R;

import static sneer.android.ui.SneerActivity.findTextView;

public class ChatAdapter extends ArrayAdapter<ChatMessage> implements OnClickListener {

	private final LayoutInflater inflater;
	private String partyNick = "";

	public ChatAdapter(Context context, LayoutInflater inflater) {
		super(context, -1, new ArrayList<ChatMessage>());
		this.inflater = inflater;
	}

	public void update(String partyNick, List<ChatMessage> messages) {
		this.partyNick = partyNick;
		this.clear();
		this.addAll(messages);
		this.notifyDataSetChanged();
	}


	@Override
	public int getViewTypeCount() {
		return 2;
	}

	@Override
	public int getItemViewType(int position) {
		return getItem(position).isOwn ? 1 : 0;
	}

	@SuppressLint("ViewHolder")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ChatMessage message = this.getItem(position);

		View view = convertView;
		if (view == null)
			view = inflater.inflate(getItemViewType(position) == 1 ? R.layout.list_item_user_message : R.layout.list_item_party_message, parent, false);

		updateView(message, view);

		return view;
	}

	private View updateView(final ChatMessage message, final View view) {
		final TextView messageView = findTextView(view, R.id.messageContent);

		SpannableString messageContent = new SpannableString(message.text);

		messageView.setAutoLinkMask(Linkify.ALL);
		messageView.setText(messageContent);

		findTextView(view, R.id.messageTime).setText(message.date);

		setColors(view, partyNick, message.isOwn);

		return view;
	}

	private void setColors(View row, String partyNick, boolean own) {
		int color;
		final LinearLayout speechBubble = (LinearLayout) row.findViewById(R.id.speechBubble);

		if (own) {
			color = darkColorDeterminedBy(partyNick);
			View arrow = row.findViewById(R.id.speechBubbleArrowRight);
			arrow.setBackground(new TriangleRightDrawable(color));  //Color.parseColor("#ce5343")
		} else {
			color = lightColorDeterminedBy(partyNick);
			View arrow = row.findViewById(R.id.speechBubbleArrowLeft);
			arrow.setBackground(new TriangleLeftDrawable(color));
		}

		LayerDrawable bubbleLayer = (LayerDrawable) speechBubble.getBackground();
		GradientDrawable bubbleBackground = (GradientDrawable) bubbleLayer.findDrawableByLayerId(R.id.bubbleBackground);
		bubbleBackground.setColor(color);

		GradientDrawable bubbleShadow = (GradientDrawable) bubbleLayer.findDrawableByLayerId(R.id.bubbleShadow);
		bubbleShadow.setColor(color);
	}

	private static int darkColorDeterminedBy(String string) {
		return colorDeterminedBy(string, 3);
	}

	private static int lightColorDeterminedBy(String string) {
		return colorDeterminedBy(string, 1);
	}

	private static int colorDeterminedBy(String string, int factor) {
		Random random = new Random(string.hashCode() * 713);
		List<Integer> rgb = new ArrayList<>(3);
		// Separation of at least 36 between red and green to avoid greys and ugly browns.
		rgb.add((180 + random.nextInt(20) + 56) / factor);
		rgb.add((180 + random.nextInt(20)) / factor);
		Collections.shuffle(rgb, random);
		rgb.add((210 + random.nextInt(46)) / factor); // Favor blue a bit.
		return Color.argb(255, rgb.get(0), rgb.get(1), rgb.get(2));
	}

	@Override
	public void onClick(View v) {
		// do nothing
	}

}
