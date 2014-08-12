package sneer.android.main;

import sneer.commons.*;
import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class SystemReportActivity extends Activity {

	TextView reportView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_system_report);
		
		reportView = (TextView)findViewById(R.id.reportView);
		
		report();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.system_report, menu);
		return true;
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
		SystemReport.updateReport("Shields On", true);
		SystemReport.updateReport("Phasers", "Stun");
		String report = SystemReport.report().toBlockingObservable().first();
		reportView.setText(report);
	}
}
