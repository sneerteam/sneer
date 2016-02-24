package sneer.android.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import sneer.commons.SystemReport;
import sneer.main.R;

public class SystemReportActivity extends SneerActionBarActivity {

	private TextView mReportView;
	private ScrollView mScrollView;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_system_report);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);     // Attaching the layout to the toolbar object
		setSupportActionBar(toolbar);                               // Setting toolbar as the ActionBar with setSupportActionBar() call
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		mReportView = (TextView)findViewById(R.id.reportView);
		mScrollView = (ScrollView)findViewById(R.id.scrollView);

		report();
	}


	private void report() {
		SystemReport.report().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {  @Override public void call(String reportMessage) {
			mReportView.setText(reportMessage);
			mScrollView.fullScroll(View.FOCUS_DOWN);
		}});
	}

}
