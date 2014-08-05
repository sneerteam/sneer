package sneer.android.main.ui;

import static sneer.android.ui.UIUtils.*;
import static sneer.android.main.ui.utils.DateUtils.*;
import rx.*;
import rx.functions.*;
import sneer.*;
import sneer.android.main.*;
import android.app.*;
import android.view.*;
import android.widget.*;

public class InteractionListAdapter extends ArrayAdapter<Interaction> {

	private Activity activity;
    int layoutResourceId;
	private final Func1<Party, Observable<String>> labelProvider;
	private final Func1<Party, Observable<byte[]>> imageProvider;
    
    public InteractionListAdapter(Activity activity, int layoutResourceId, Func1<Party, Observable<String>> labelProvider, Func1<Party, Observable<byte[]>> imageProvider) {
        super(activity, layoutResourceId);
        this.layoutResourceId = layoutResourceId;
        this.activity = activity;
		this.labelProvider = labelProvider;
		this.imageProvider = imageProvider;
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
            
            row.setTag(holder);
        } else {
            holder = (InteractiontHolder)row.getTag();
        }
        
        Interaction interaction = getItem(position);
        plug(holder.interactionParty, labelProvider.call(interaction.party()));
        plug(holder.interactionSummary, interaction.mostRecentEventContent().observable());
        plug(holder.interactionPicture, imageProvider.call(interaction.party()));
        plugDate(holder.interactionDate, interaction.mostRecentEventTimestamp().observable());
        
        return row;
    }

	static class InteractiontHolder
    {
        TextView interactionParty;
        TextView interactionSummary;
        TextView interactionDate;
        ImageView interactionPicture;
    }
}