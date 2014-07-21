package sneer.android.main.ui;

import static sneer.android.ui.UIUtils.*;
import sneer.*;
import sneer.android.main.*;
import android.app.*;
import android.view.*;
import android.widget.*;

public class ContactsAdapter extends ArrayAdapter<Contact> {

	private Activity activity;
    int layoutResourceId;
    
    public ContactsAdapter(Activity activity, int layoutResourceId) {
        super(activity, layoutResourceId);
        this.layoutResourceId = layoutResourceId;
        this.activity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        final ContactHolder holder;
        
        if (row == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            
            holder = new ContactHolder();
            holder.contactNickName = (TextView)row.findViewById(R.id.contactNickName);
            
            row.setTag(holder);
        } else {
            holder = (ContactHolder)row.getTag();
        }
        
        Contact contact = getItem(position);
//        subscribeTextView(holder.contactNickName, contact.nickname().mostRecent());
        
        return row;
    }

	static class ContactHolder
    {
        TextView contactNickName;
    }
}