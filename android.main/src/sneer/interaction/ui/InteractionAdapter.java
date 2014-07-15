package sneer.interaction.ui;

import java.util.*;

import sneer.*;
import sneer.android.main.*;
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
    
    public InteractionAdapter(Context context, LayoutInflater inflater, int layoutUserResourceId, int listContactResourceId, List<InteractionEvent> data) {
        super(context, layoutUserResourceId, data);
		this.inflater = inflater;
        this.layoutUserResourceId = layoutUserResourceId;
        this.listContactResourceId = listContactResourceId;
        this.data = data;
    }

    @SuppressLint("NewApi")
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row;
        
        InteractionEvent interactionEvent = data.get(position);
        
        String sender = interactionEvent.sender().nickname().toBlockingObservable().first();

        int resourceId = interactionEvent.isOwn() ? layoutUserResourceId : listContactResourceId;
        row = inflater.inflate(resourceId, parent, false);
        
        RelativeLayout speechBubble = (RelativeLayout)row.findViewById(R.id.speechBubble);
        TextView interactionEventContent = (TextView)row.findViewById(R.id.interactionEventContent);
        TextView interactionEventSender = (TextView)row.findViewById(R.id.interactionEventSender);
        TextView interactionEventTime = (TextView)row.findViewById(R.id.interactionEventTime);
        
        interactionEventContent.setText(interactionEvent.content());
		interactionEventSender.setText(sender);
        if (interactionEvent.isOwn()) {
        	View speechBubbleArrowRight = row.findViewById(R.id.speechBubbleArrowRight);
        	speechBubbleArrowRight.setBackground(new TriangleRightDrawable(Color.parseColor("#D34F39")));
        } else {
        	interactionEventSender.setTextColor(darkColorDeterminedBy(sender));
        	
        	View speechBubbleArrowLeft = row.findViewById(R.id.speechBubbleArrowLeft);
        	speechBubbleArrowLeft.setBackground(new TriangleLeftDrawable(darkColorDeterminedBy(sender)));
        	
        	LayerDrawable bubbleLayer = (LayerDrawable) speechBubble.getBackground();
        	GradientDrawable bubbleBackground = (GradientDrawable) bubbleLayer.findDrawableByLayerId(R.id.bubbleBackground);
        	bubbleBackground.setColor(lightColorDeterminedBy(sender));
        	
        	GradientDrawable bubbleShadow = (GradientDrawable) bubbleLayer.findDrawableByLayerId(R.id.bubbleShadow);
        	bubbleShadow.setColor(darkColorDeterminedBy(sender));
        }
        
        interactionEventTime.setText(interactionEvent.timeSent());
        
        return row;
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