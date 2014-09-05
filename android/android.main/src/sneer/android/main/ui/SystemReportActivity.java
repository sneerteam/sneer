package sneer.android.main.ui;

import rx.android.schedulers.*;
import rx.functions.*;
import sneer.android.main.*;
import sneer.commons.*;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class SystemReportActivity extends Activity {

	TextView mReportView;
	ScrollView mScrollView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_system_report);
		
		mReportView = (TextView)findViewById(R.id.reportView);
		mScrollView = (ScrollView) findViewById(R.id.scrollView);
		
		report();
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	
	private void report() {
		SystemReport.report().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<String>() {  @Override public void call(String reportMessage) {
			mReportView.setText(reportMessage);
			mScrollView.fullScroll(View.FOCUS_DOWN);
		}});
	}
}
