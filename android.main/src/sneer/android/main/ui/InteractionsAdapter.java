package sneer.android.main.ui;

import static sneer.android.ui.UIUtils.*;
import sneer.*;
import sneer.android.main.*;
import android.app.*;
import android.view.*;
import android.widget.*;

public class InteractionsAdapter extends ArrayAdapter<Interaction> {

	private Activity activity;
    int layoutResourceId;
    
    public InteractionsAdapter(Activity activity, int layoutResourceId) {
        super(activity, layoutResourceId);
        this.layoutResourceId = layoutResourceId;
        this.activity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        final InteractiontHolder holder;
        
        if (row == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            
            holder = new InteractiontHolder();
            holder.interactionSummary = (TextView)row.findViewById(R.id.interactionSummary);
            
            row.setTag(holder);
        } else {
            holder = (InteractiontHolder)row.getTag();
        }
        
        Interaction interaction = getItem(position);
        subscribeTextView(holder.interactionSummary, interaction.party().name());
        
        return row;
    }

	static class InteractiontHolder
    {
        TextView interactionSummary;
    }
}