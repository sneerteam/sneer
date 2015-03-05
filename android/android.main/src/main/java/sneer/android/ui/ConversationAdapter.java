package sneer.android.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import rx.functions.Action1;
import sneer.Message;
import sneer.Party;
import sneer.R;
import sneer.android.ui.drawable.TriangleLeftDrawable;
import sneer.android.ui.drawable.TriangleRightDrawable;

import java.util.List;
import java.util.Random;

import static sneer.android.SneerAndroidSingleton.sneerAndroid;
import static sneer.android.ui.SneerActivity.findImageView;
import static sneer.android.ui.SneerActivity.findTextView;

class ConversationAdapter extends ArrayAdapter<Message> implements OnClickListener{

	private final int layoutUserResourceId;
    private final int listContactResourceId;
	private final LayoutInflater inflater;
	private final Party party;

    ConversationAdapter(Context context,
    		LayoutInflater inflater,
    		int layoutUserResourceId,
    		int listContactResourceId,
    		List<Message> messages,
    		Party party) {
        super(context, layoutUserResourceId, messages);
		this.inflater = inflater;
        this.layoutUserResourceId = layoutUserResourceId;
        this.listContactResourceId = listContactResourceId;
		this.party = party;
    }


	@SuppressLint("ViewHolder") @Override
    public View getView(int position, View convertView, ViewGroup parent) {
		final Message message = this.getItem(position);
        final View ret = inflater.inflate(
        	message.isOwn() ? layoutUserResourceId : listContactResourceId,
        	parent,
        	false);

        final TextView messageView = findTextView(ret, R.id.messageContent);
        final ImageView imageView = findImageView(ret, R.id.img);

        SpannableString messageContent = new SpannableString(message.label());
        byte[] messageImage = message.jpegImage();

        if (messageImage != null)
        	imageView.setImageBitmap(BitmapFactory.decodeByteArray(messageImage, 0, messageImage.length));

        messageView.setAutoLinkMask(Linkify.ALL);
        findTextView(ret, R.id.messageTime).setText(message.timeCreated());

        ret.setTag(message);
        if (sneerAndroid().isClickable(message)) {
        	messageView.setAutoLinkMask(0);
	        ret.setClickable(true);
	        ret.setOnClickListener(this);
	        clickableMessageStyle(messageView, messageContent);
        }

        messageView.setText(messageContent);

        party.name().subscribe(new Action1<String>() { @Override public void call(String sender) {
        	setColors(ret, sender, message.isOwn());
        }});

       	return ret;
    }


	private void clickableMessageStyle(final TextView messageView, SpannableString messageContent) {
		messageContent.setSpan(new UnderlineSpan(), 0, messageContent.length(), 0);
		messageView.setTextColor(Color.rgb(52, 183, 228));
	}


	private void setColors(View row, String sender, boolean own) {
		final RelativeLayout speechBubble = (RelativeLayout)row.findViewById(R.id.speechBubble);
		if (own) {
			View speechBubbleArrowRight = row.findViewById(R.id.speechBubbleArrowRight);
			speechBubbleArrowRight.setBackground(new TriangleRightDrawable(Color.parseColor("#D34F39")));
		} else {
			View speechBubbleArrowLeft = row.findViewById(R.id.speechBubbleArrowLeft);
			speechBubbleArrowLeft.setBackground(new TriangleLeftDrawable(darkColorDeterminedBy(sender)));

			LayerDrawable bubbleLayer = (LayerDrawable) speechBubble.getBackground();
			GradientDrawable bubbleBackground = (GradientDrawable) bubbleLayer.findDrawableByLayerId(R.id.bubbleBackground);
			bubbleBackground.setColor(lightColorDeterminedBy(sender));

			GradientDrawable bubbleShadow = (GradientDrawable) bubbleLayer.findDrawableByLayerId(R.id.bubbleShadow);
			bubbleShadow.setColor(darkColorDeterminedBy(sender));
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
		sneerAndroid().doOnClick((Message) v.getTag());
	}

}