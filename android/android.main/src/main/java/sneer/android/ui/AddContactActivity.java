package sneer.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.android.SneerAndroidContainer;
import sneer.android.utils.AndroidUtils;
import sneer.Contact;

import sneer.convos.Convos;
import sneer.main.R;
import sneer.Party;
import sneer.commons.exceptions.FriendlyException;

import static sneer.android.SneerAndroidSingleton.sneer;
import static sneer.android.ui.SneerActivity.ui;
import static sneer.android.utils.Puk.shareOwnPublicKey;

public class AddContactActivity extends Activity {

	private Party party;

	private EditText nicknameEdit;
	private Button btnSendInvite;

	private String nickname;
    private Convos convos;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_contact);

        convos = SneerAndroidContainer.component(Convos.class);

		nicknameEdit = (EditText) findViewById(R.id.nickname);
		btnSendInvite = (Button) findViewById(R.id.btn_send_invite);
		btnSendInvite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
                convos.startConvo(nickname).subscribe(new Subscriber<Long>() {
                    @Override
                    public final void onCompleted() {
                    }
                    @Override
                    public final void onError(Throwable e) {
                        AndroidUtils.toast(AddContactActivity.this, "TOAST: " + e.getMessage(), Toast.LENGTH_LONG);
                    }
                    @Override
                    public final void onNext(Long convoId) {
                        InviteSender.send(AddContactActivity.this, convoId);
                        finish();
                    }
                });
			}
		});

		validationOnTextChanged(nicknameEdit);
	}

	private void validationOnTextChanged(final EditText editText) {
		editText.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				nickname = nicknameEdit.getText().toString();
				ui(convos.problemWithNewNickname(nickname)).subscribe(new Action1<String>() {
                    @Override
                    public void call(String error) {
                        refreshNicknameProblem(error);
                    }
                });
			}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
		});
	}

    private void refreshNicknameProblem(String error) {
        if (error != null) nicknameEdit.setError(error);
        btnSendInvite.setEnabled(nickname.length() != 0 && error == null);
    }

}
