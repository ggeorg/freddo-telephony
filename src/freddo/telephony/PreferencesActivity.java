package freddo.telephony;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import freddo.dtalk.DTalkService;
import freddo.dtalk.util.LOG;
import freddo.telephony.R;

@SuppressLint("NewApi")
public class PreferencesActivity extends PreferenceActivity {
	private final String TAG = LOG.tag(PreferencesActivity.class);

	public static final String PREF_APP_VERSION = "app_version";

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		LOG.v(TAG, ">>> onCreate");
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		// Update target Name...
		EditTextPreference targetNamePref = (EditTextPreference) findPreference(Constants.PREF_TARGET_NAME);
		targetNamePref.setText(DTalkService.getInstance().getConfiguration()
				.getTargetName());

		// Update APP VERSION...
		EditTextPreference versionPref = (EditTextPreference) findPreference(Constants.PREF_APP_VERSION);
		versionPref.setTitle(getString(R.string.version,
				getAppVersionName(this)));
	}

	/**
	 * Gets the version of app.
	 * 
	 * @param context
	 * @return
	 */
	public static String getAppVersionName(Context context) {
		String versionString = null;
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			versionString = info.versionName;
		} catch (Exception e) {
			// do nothing
		}
		return versionString;
	}

	/**
	 * Saves a string value under the provided key in the preference manager. If
	 * <code>value</code> is <code>null</code>, then the provided key will be
	 * removed from the preferences.
	 * 
	 * @param context
	 * @param key
	 * @param value
	 */
	public static void saveStringToPreference(Context context, String key,
			String value) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (null == value) {
			// we want to remove
			pref.edit().remove(key).apply();
		} else {
			pref.edit().putString(key, value).apply();
		}
	}
}