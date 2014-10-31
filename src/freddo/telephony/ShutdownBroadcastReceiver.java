package freddo.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ShutdownBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		stopService(context);
	}

	/**
	 * Method to stop the service.
	 */
	protected void stopService(Context context) {
		if (FreddoTelephonyApp.isServiceRunning(context,
				FreddoTelephonyService.class)) {
			context.stopService(new Intent(context,
					FreddoTelephonyService.class));
		}
	}

}
