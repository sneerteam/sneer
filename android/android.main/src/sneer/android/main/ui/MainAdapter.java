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

public class MainAdapter extends ArrayAdapter<Conversation> {

	private Activity activity;
    int layoutResourceId;
	private final Func1<Party, Observable<String>> labelProvider;
	private final Func1<Party, Observable<byte[]>> imageProvider;
	private CompositeSubscription subscriptions;
    
	public MainAdapter(Activity activity, int layoutResourceId, Func1<Party, Observable<String>> labelProvider, Func1<Party, Observable<byte[]>> imageProvider) {
        super(activity, layoutResourceId);
        this.layoutResourceId = layoutResourceId;
        this.imageProvider = imageProvider;
        this.activity = activity;
		this.labelProvider = labelProvider;
		this.subscriptions = new CompositeSubscription();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        final ConversationtHolder holder;
        
        if (row == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            
            holder = new ConversationtHolder();
            holder.conversationParty = findView(row, R.id.conversationParty);
            holder.conversationSummary = findView(row, R.id.conversationSummary);
            holder.conversationDate = findView(row, R.id.conversationDate);
            holder.conversationPicture = findView(row, R.id.conversationPicture);
            holder.conversationUnread = findView(row, R.id.conversationUnread);
            
            Shader textShader = new LinearGradient(200, 0, 650, 0, 
            		new int[] {Color.DKGRAY, Color.LTGRAY},
            		new float[] {0, 1}, TileMode.CLAMP);
            holder.conversationSummary.getPaint().setShader(textShader);
            
            row.setTag(holder);
        } else {
            holder = (ConversationtHolder)row.getTag();
        }
        
		Conversation conversation = getItem(position);
		Subscription subscription = Subscriptions.from(
				plug(holder.conversationParty, labelProvider.call(conversation.party())),
				plug(holder.conversationSummary, conversation.mostRecentMessageContent().observable()),
				plug(holder.conversationPicture, imageProvider.call(conversation.party())),
				plugUnreadMessage(holder.conversationUnread, conversation.unreadMessageCount()),
				plugDate(holder.conversationDate, conversation.mostRecentMessageTimestamp().observable()));
		subscriptions.add(subscription);
        return row;
    }


    static class ConversationtHolder {
		TextView conversationParty;
		TextView conversationSummary;
		TextView conversationDate;
		TextView conversationUnread;
		ImageView conversationPicture;
	}

	
	public static Subscription plugUnreadMessage(final TextView textView, Observable<Long> observable) {
		return deferUI(observable).subscribe(new Action1<Long>() { @Override public void call(Long obj) {
			if (obj == 0)
				textView.setVisibility(View.GONE);
			else
				textView.setVisibility(View.VISIBLE);
			textView.setText(obj.toString());
		}});
	}


	public void dispose() {
		subscriptions.unsubscribe();
	}
}