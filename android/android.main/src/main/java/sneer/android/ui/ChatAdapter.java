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
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import sneer.android.ui.drawable.TriangleLeftDrawable;
import sneer.android.ui.drawable.TriangleRightDrawable;
import sneer.convos.ChatMessage;
import sneer.main.R;

import static sneer.android.ui.SneerActivity.findTextView;

public class ChatAdapter extends ArrayAdapter<ChatMessage> implements OnClickListener{

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

	@SuppressLint("ViewHolder") @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ChatMessage message = this.getItem(position);

        View view = convertView == null
                ? inflater.inflate(message.isOwn ? R.layout.list_item_user_message : R.layout.list_item_party_message, parent, false)
                : convertView;

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
		final RelativeLayout speechBubble = (RelativeLayout)row.findViewById(R.id.speechBubble);
		if (own) {
			View speechBubbleArrowRight = row.findViewById(R.id.speechBubbleArrowRight);
			speechBubbleArrowRight.setBackground(new TriangleRightDrawable(Color.parseColor("#ce5343")));
		} else {
			View speechBubbleArrowLeft = row.findViewById(R.id.speechBubbleArrowLeft);
			speechBubbleArrowLeft.setBackground(new TriangleLeftDrawable(darkColorDeterminedBy(partyNick)));

			LayerDrawable bubbleLayer = (LayerDrawable) speechBubble.getBackground();
			GradientDrawable bubbleBackground = (GradientDrawable) bubbleLayer.findDrawableByLayerId(R.id.bubbleBackground);
			bubbleBackground.setColor(lightColorDeterminedBy(partyNick));

			GradientDrawable bubbleShadow = (GradientDrawable) bubbleLayer.findDrawableByLayerId(R.id.bubbleShadow);
			bubbleShadow.setColor(darkColorDeterminedBy(partyNick));
		}
	}

	private static int darkColorDeterminedBy(String string) {
		return colorDeterminedBy(string, 50);
	}

	private static int lightColorDeterminedBy(String string) {
		return colorDeterminedBy(string, 170);
	}

	private static int colorDeterminedBy(String string, int strength) {
		Random random = new Random(string.hashCode() * 713);
		int r = strength + random.nextInt(86);
		int g = strength + random.nextInt(86);
		int b = strength + random.nextInt(86);
		return Color.argb(255, r, g, b);
	}

    @Override
    public void onClick(View v) {
        // do nothing
    }

}
