package sneer.android.main.ui.interactions;

import static android.os.Build.VERSION.*;
import static android.os.Build.VERSION_CODES.*;
import static sneer.android.ui.UIUtils.*;

import java.util.*;

import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import sneer.android.ui.*;
import sneer.rx.*;
import android.annotation.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

public class InteractionAdapter extends ArrayAdapter<InteractionEvent>{

    int layoutUserResourceId;    
    int listContactResourceId;
    List<InteractionEvent> data = null;
	private LayoutInflater inflater;
	private Sneer sneer;
    
    public InteractionAdapter(Context context, LayoutInflater inflater, int layoutUserResourceId, int listContactResourceId, List<InteractionEvent> data, Sneer sneer) {
        super(context, layoutUserResourceId, data);
		this.inflater = inflater;
        this.layoutUserResourceId = layoutUserResourceId;
        this.listContactResourceId = listContactResourceId;
        this.data = data;
		this.sneer = sneer;
    }

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		final InteractionEvent event = data.get(position);
        final View ret = inflater.inflate(
        	event.isOwn() ? layoutUserResourceId : listContactResourceId,
        	parent,
        	false);
        
        findText(ret, R.id.interactionEventContent).setText(event.content());
        findText(ret, R.id.interactionEventTime).setText(event.timeSent());

        sneer.labelFor(event.sender()).observable().subscribe(new Action1<String>() { @Override public void call(String sender) { 
        	TextView senderView = findText(ret, R.id.interactionEventSender);
        	senderView.setText(sender);
        	setColors(senderView, ret, sender, event.isOwn());
        }}); 
        
        return ret;
    }

	@SuppressLint("NewApi")
	private void setColors(TextView interactionEventSender, View row, String sender, boolean own) {
		final RelativeLayout speechBubble = (RelativeLayout)row.findViewById(R.id.speechBubble);
		if (own) {
			View speechBubbleArrowRight = row.findViewById(R.id.speechBubbleArrowRight);
			if (SDK_INT >= ICE_CREAM_SANDWICH){
				speechBubbleArrowRight.setBackground(new TriangleRightDrawable(Color.parseColor("#D34F39")));
			} else {
				int drawTriangleRight;
			}
		} else {
			interactionEventSender.setTextColor(darkColorDeterminedBy(sender));
			
			View speechBubbleArrowLeft = row.findViewById(R.id.speechBubbleArrowLeft);
			if (SDK_INT >= ICE_CREAM_SANDWICH){
				speechBubbleArrowLeft.setBackground(new TriangleLeftDrawable(darkColorDeterminedBy(sender)));
			} else {
				int drawTriangleLeft;
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
}