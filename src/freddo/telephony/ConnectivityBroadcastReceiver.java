package freddo.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public class ConnectivityBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		NetworkInfo info = (NetworkInfo) intent
				.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		if (info.getState().equals(NetworkInfo.State.CONNECTING)) {
			startService(context);
		} else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
			startService(context);
		} else if (info.getState().equals(NetworkInfo.State.DISCONNECTING)) {
			//stopService(context);
		} else if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
			//stopService(context);
		}
	}

	/**
	 * Method to start the service.
	 */
	protected void startService(Context context) {
		if (!FreddoTelephonyApp.isServiceRunning(context,
				FreddoTelephonyService.class)) {
			context.startService(new Intent(context,
					FreddoTelephonyService.class));
		}
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
