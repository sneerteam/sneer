package sneer.android.ui;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import sneer.android.R;
import sneer.commons.SystemReport;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class SystemReportActivity extends Activity {

	private TextView mReportView;
	private ScrollView mScrollView;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_system_report);
		
		mReportView = (TextView)findViewById(R.id.reportView);
		mScrollView = (ScrollView) findViewById(R.id.scrollView);
		
		report();
	}


	private void report() {
		SystemReport.report().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {  @Override public void call(String reportMessage) {
			mReportView.setText(reportMessage);
			mScrollView.fullScroll(View.FOCUS_DOWN);
		}});
	}
	
}
