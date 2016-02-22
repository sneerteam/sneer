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
import sneer.convos.Convos;
import sneer.main.R;

import static sneer.android.ui.SneerActivity.ui;
import static sneer.android.utils.AndroidUtils.finishWith;
import static sneer.android.utils.AndroidUtils.toastOnMainThread;

public class AcceptInviteActivity extends Activity {

    public static final String REFERRER_CODE = "REFERRER_CODE";

    private EditText nicknameEdit;
	private Button btnDone;

	private String nickname;
    private Convos convos;
    private String inviteCode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		inviteCode = getInviteCode();

		if (inviteCode == null) {
			finishWith("Invalid Invite", this);
			return;
		}

		convos = SneerAndroidContainer.component(Convos.class);

		ui(convos.findConvo(inviteCode)).subscribe(new Action1<Long>() { @Override public void call(Long convoId) {
			if (convoId == null)
				obtainNickname();
			else
				navigateToConvo(convoId);
		}});
	}

    private String getInviteCode() {
        String referrerCode = getIntent().getStringExtra(REFERRER_CODE);
        return referrerCode != null
            ? referrerCode
            : getURIQuery(getIntent());
    }

    private void obtainNickname() {
		setContentView(R.layout.activity_add_contact);

		nicknameEdit = (EditText) findViewById(R.id.nickname);
		btnDone = (Button) findViewById(R.id.btn_done);

		btnDone.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
			convos.acceptInvite(getNickname(), inviteCode).subscribe(new Subscriber<Long>() {
				@Override
				public final void onCompleted() {}

				@Override
				public final void onError(Throwable e) {
					toastOnMainThread(AcceptInviteActivity.this, e.getMessage(), Toast.LENGTH_LONG);
				}

				@Override
				public final void onNext(Long convoId) {
					navigateToConvo(convoId);
				}
			});
		}});

		setNicknameValidationOnTextChanged(nicknameEdit);
	}

	private void navigateToConvo(Long convoId) {
		ConvoActivity.open(AcceptInviteActivity.this, convoId);
		finish();
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

    private String getURIQuery(Intent intent) {
	    return Intent.ACTION_VIEW.equals(intent.getAction())
			? intent.getData().getQuery()
			: null;
    }

    private String getNickname() {
        return nicknameEdit.getText().toString().trim();
    }

}
