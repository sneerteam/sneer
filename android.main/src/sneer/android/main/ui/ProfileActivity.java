package sneer.android.main.ui;

import sneer.*;
import sneer.android.main.R;
import sneer.snapi.*;
import android.content.*;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.*;
import android.preference.*;

public class ProfileActivity extends PreferenceActivity {

	private Sneer cloud;
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setupSimplePreferencesScreen();
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//		cloud = Cloud.cloudFor(this);
		prefs.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
			@Override
			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String field) {
//				cloud.path("info", field).pub(sharedPreferences.getString(field, null));
			}
		});
	}
	
	@Override
	protected void onDestroy() {
//		cloud.dispose();
		super.onDestroy();
	}
	
	@SuppressWarnings("deprecation")
	private void setupSimplePreferencesScreen() {
		addPreferencesFromResource(R.xml.pref_general);
		bindPreferenceSummaryToValue(findPreference("name"));
		bindPreferenceSummaryToValue(findPreference("nickname"));
	}

	private static Preference.OnPreferenceChangeListener S_BIND_PREFERENCE_SUMMARY_TO_VALUE_LISTENER = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
			} else {
				preference.setSummary(stringValue);
			}
			return true;
		}
	};
	
	private static void bindPreferenceSummaryToValue(Preference preference) {
		preference.setOnPreferenceChangeListener(S_BIND_PREFERENCE_SUMMARY_TO_VALUE_LISTENER);
		S_BIND_PREFERENCE_SUMMARY_TO_VALUE_LISTENER.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
	}

}
