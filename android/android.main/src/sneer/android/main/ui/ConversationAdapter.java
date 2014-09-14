package sneer.android.main.ui;

import static android.os.Build.VERSION.*;
import static android.os.Build.VERSION_CODES.*;
import static sneer.android.ui.SneerActivity.*;

import java.util.*;

import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.android.main.ui.drawable.*;
import android.annotation.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

public class ConversationAdapter extends ArrayAdapter<Message> implements OnClickListener{

    int layoutUserResourceId;    
    int listContactResourceId;
	private LayoutInflater inflater;
	private Party party;
    
    public ConversationAdapter(Context context,
    		LayoutInflater inflater,
    		int layoutUserResourceId,
    		int listContactResourceId,
    		List<Message> data,
    		Party party) {
        super(context, layoutUserResourceId, data);
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
        
        findTextView(ret, R.id.messageContent).setText(message.content().toString());
        findTextView(ret, R.id.messageTime).setText(message.timeCreated());
        
        ret.setTag(message);
        ret.setClickable(true);
        ret.setOnClickListener(this);
        
        if (!message.isOwn()) {
        	party.name().subscribe(new Action1<String>() { @Override public void call(String sender) { 
        		setColors(ret, sender, message.isOwn());
        	}});
        } else {
			setColors(ret, null, true);
        }
        
       	return ret;
    }

	@SuppressLint("NewApi")
	private void setColors(View row, String sender, boolean own) {
		final RelativeLayout speechBubble = (RelativeLayout)row.findViewById(R.id.speechBubble);
		if (own) {
			View speechBubbleArrowRight = row.findViewById(R.id.speechBubbleArrowRight);
			if (SDK_INT >= ICE_CREAM_SANDWICH){
				speechBubbleArrowRight.setBackground(new TriangleRightDrawable(Color.parseColor("#D34F39")));
			} 
			
		} else {
			View speechBubbleArrowLeft = row.findViewById(R.id.speechBubbleArrowLeft);
			if (SDK_INT >= ICE_CREAM_SANDWICH){
				speechBubbleArrowLeft.setBackground(new TriangleLeftDrawable(darkColorDeterminedBy(sender)));
			} 
			
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
		((Message) v.getTag()).click();
	}
}