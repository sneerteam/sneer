package sneer.android.main.ui;

import static sneer.android.ui.SneerActivity.*;
import rx.*;
import rx.functions.*;
import rx.subscriptions.*;
import sneer.*;
import sneer.android.main.*;
import android.app.*;
import android.graphics.*;
import android.graphics.Shader.TileMode;
import android.view.*;
import android.widget.*;

public class MainAdapter extends ArrayAdapter<Interaction> {

	private Activity activity;
    int layoutResourceId;
	private final Func1<Party, Observable<String>> labelProvider;
	private final Func1<Party, Observable<byte[]>> imageProvider;
	private CompositeSubscription subscriptions;
    
    public MainAdapter(Activity activity, int layoutResourceId, Func1<Party, Observable<String>> labelProvider, Func1<Party, Observable<byte[]>> imageProvider) {
        super(activity, layoutResourceId);
        this.layoutResourceId = layoutResourceId;
        this.activity = activity;
		this.labelProvider = labelProvider;
		this.imageProvider = imageProvider;
		this.subscriptions = new CompositeSubscription();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        final InteractiontHolder holder;
        
        if (row == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            
            holder = new InteractiontHolder();
            holder.interactionParty = findView(row, R.id.interactionParty);
            holder.interactionSummary = findView(row, R.id.interactionSummary);
            holder.interactionDate = findView(row, R.id.interactionDate);
            holder.interactionPicture = findView(row, R.id.interactionPicture);
            
            Shader textShader = new LinearGradient(200, 0, 650, 0, 
            		new int[] {Color.DKGRAY, Color.LTGRAY},
            		new float[] {0, 1}, TileMode.CLAMP);
            holder.interactionSummary.getPaint().setShader(textShader);
            
            row.setTag(holder);
        } else {
            holder = (InteractiontHolder)row.getTag();
        }
        
		Interaction interaction = getItem(position);
		Subscription subscription = Subscriptions.from(
				plug(holder.interactionParty, labelProvider.call(interaction.party())),
				plug(holder.interactionSummary, interaction.mostRecentEventContent().observable()),
				plug(holder.interactionPicture, imageProvider.call(interaction.party())),
				plugDate(holder.interactionDate, interaction.mostRecentEventTimestamp().observable()));
		subscriptions.add(subscription);
        return row;
    }

    
    
	static class InteractiontHolder
    {
        TextView interactionParty;
        TextView interactionSummary;
        TextView interactionDate;
        ImageView interactionPicture;
    }



	public void dispose() {
		subscriptions.unsubscribe();
	}
}