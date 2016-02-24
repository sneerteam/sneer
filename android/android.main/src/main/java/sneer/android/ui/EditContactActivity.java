package sneer.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import rx.Subscriber;
import rx.functions.Action1;
import sneer.convos.Convo;
import sneer.convos.Convos;
import sneer.main.R;

import static sneer.android.SneerAndroidContainer.component;
import static sneer.android.SneerAndroidFlux.dispatch;
import static sneer.android.ui.SneerActivity.ui;
import static sneer.android.utils.AndroidUtils.toastOnMainThread;

public class EditContactActivity extends Activity {

	private EditText nicknameEdit;
	private Button btnDone;

	private String nickname;
	private String oldNickname;
	private final Convos convos = component(Convos.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_contact);

		final long convoId = getIntent().getLongExtra("convoId", -1);
		oldNickname = getIntent().getStringExtra("oldNickname");

		nicknameEdit = (EditText) findViewById(R.id.nickname);
		nicknameEdit.setText(oldNickname);
		nicknameEdit.selectAll();

		btnDone = (Button) findViewById(R.id.btn_done);
		btnDone.setEnabled(false);

		btnDone.setText("DONE >");

		btnDone.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				convos.getById(convoId).subscribe(new Subscriber<Convo>() {
					@Override
					public void onCompleted() {	}

					@Override
					public void onError(Throwable throwable) {
						toastOnMainThread(EditContactActivity.this, throwable.getMessage(), Toast.LENGTH_LONG);
					}

					@Override
					public void onNext(Convo convo) {
						dispatch(convo.setNickname(nickname));
						unsubscribe();
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

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
		});
	}

	private void refreshNicknameProblem(String error) {
		if (!nickname.isEmpty() && !nickname.equals(oldNickname) && error != null) nicknameEdit.setError(error);
		btnDone.setEnabled(!nickname.isEmpty() && error == null);
	}

}
