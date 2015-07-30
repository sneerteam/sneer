package sneer.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import rx.Subscriber;
import rx.functions.Action1;
import sneer.android.SneerAndroidContainer;
import sneer.android.utils.AndroidUtils;
import sneer.convos.Convos;
import sneer.main.R;

import static sneer.android.ui.SneerActivity.ui;

public class AcceptInviteActivity extends Activity {

	private EditText nicknameEdit;
	private Button btnDone;

	private String nickname;
    private Convos convos;
    private String contactPuk;
    private String inviteCode;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        parseQuery(getIntent());

        if (contactPuk == null || inviteCode == null) {
            AndroidUtils.finishWith("Invalid Invite", this);
			return;
		}

		convos = SneerAndroidContainer.component(Convos.class);

		// TODO Call Convos.findConvo() with puk and, if found, open ConvoActiviy then finish
		convos.findConvo(contactPuk).subscribe(new Action1<Long>() {
			@Override
			public void call(Long convoId) {
				ConvoActivity.open(AcceptInviteActivity.this, convoId);
                finish();
			}
		});

		setContentView(R.layout.activity_add_contact);


		nicknameEdit = (EditText) findViewById(R.id.nickname);
		btnDone = (Button) findViewById(R.id.btn_done);

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                convos.acceptInvite(getNickname(), contactPuk, inviteCode).subscribe(new Subscriber<Long>() {
                    @Override
                    public final void onCompleted() {
                    }

                    @Override
                    public final void onError(Throwable e) {
                        AndroidUtils.toast(AcceptInviteActivity.this, e.getMessage(), Toast.LENGTH_LONG);
                    }

                    @Override
                    public final void onNext(Long convoId) {
                        ConvoActivity.open(AcceptInviteActivity.this, convoId);
                        finish();
                    }
                });
            }
        });

		setNicknameValidationOnTextChanged(nicknameEdit);
	}

	private void setNicknameValidationOnTextChanged(final EditText editText) {
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
        if (!nickname.isEmpty() && error != null) nicknameEdit.setError(error);
        btnDone.setEnabled(!nickname.isEmpty() && error == null);
    }

    private void parseQuery(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String[] query = intent.getData().getQuery().split("&invite=");
            if (query.length != 2) return;
            contactPuk = query[0];
            inviteCode = query[1];
        }
    }

    private String getNickname() {
        return nicknameEdit.getText().toString().trim();
    }

}
