package sneer.android.main.ui;

import sneer.*;
import sneer.android.main.R;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;

public class ContactAddHelper {
	
	public interface AddListener {
		void add(OldContact contact);
	}
	
	public ContactAddHelper(ManagedContactsActivity context, final AddListener addListener) {
		View addContactView = View.inflate(context, R.layout.activity_contact_add, null);
		final EditText publicKeyEdit = (EditText) addContactView.findViewById(R.id.public_key);
		final EditText nicknameEdit = (EditText) addContactView.findViewById(R.id.nickname);
		AlertDialog alertDialog = new AlertDialog.Builder(context)
			.setView(addContactView)
			.setTitle(R.string.action_add_contact)
			.setNegativeButton("Cancel", null)
			.setPositiveButton("Add", new DialogInterface.OnClickListener() { public void onClick(DialogInterface dialog, int id) {
				addListener.add(new OldContact(publicKeyEdit.getText().toString(), nicknameEdit.getText().toString()));
			}})
			.create();
		alertDialog.show();
	}

}
