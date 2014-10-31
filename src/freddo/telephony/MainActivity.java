package freddo.telephony;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import freddo.dtalk.util.LOG;

public class MainActivity extends Activity {
	private static final String TAG = LOG.tag(MainActivity.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		startService();
	}

	/**
	 * Method to start the service.
	 */
	protected void startService() {
		if (!FreddoTelephonyApp.isServiceRunning(this, FreddoTelephonyService.class)) {
			startService(new Intent(getBaseContext(),
					FreddoTelephonyService.class));
		}
	}

	/**
	 * Method to stop the service.
	 */
	protected void stopService() {
		if (FreddoTelephonyApp.isServiceRunning(this, FreddoTelephonyService.class)) {
			stopService(new Intent(getBaseContext(),
					FreddoTelephonyService.class));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem startServiceMI = menu.findItem(R.id.action_start_service);
		MenuItem stopServiceMI = menu.findItem(R.id.action_stop_service);
		if (FreddoTelephonyApp.isServiceRunning(this, FreddoTelephonyService.class)) {
			startServiceMI.setEnabled(false);
			stopServiceMI.setEnabled(true);
		} else {
			startServiceMI.setEnabled(true);
			stopServiceMI.setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		LOG.v(TAG, ">>> onOptionsItemSelected: %s", item);

		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_start_service) {
			startService();
		} else if (id == R.id.action_stop_service) {
			stopService();
		} else if (id == R.id.action_settings) {
			Intent intent = new Intent();
			intent.setClass(MainActivity.this, PreferencesActivity.class);
			startActivityForResult(intent, 0);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
