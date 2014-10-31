package freddo.telephony;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.arkasoft.freddo.dtalk.netty4.server.DTalkNettyServerHandler;
import com.arkasoft.freddo.jmdns.JmDNS;

import freddo.dtalk.DTalkService;
import freddo.dtalk.JmDNSDTalkServiceConfiguration;
import freddo.dtalk.adnroid.AssetFileRequestHandler;
import freddo.dtalk.util.AndroidLogger;
import freddo.dtalk.util.LOG;

public class FreddoTelephonyApp extends Application implements
		OnSharedPreferenceChangeListener {
	private static final String TAG = LOG.tag(FreddoTelephonyApp.class);

	static {
		LOG.setLogLevel(LOG.VERBOSE);
		LOG.setLogger(new AndroidLogger());
	}

	private JmDNS mJmDNS = null;
	private MulticastLock multicastLock = null;

	@Override
	public void onCreate() {
		LOG.v(TAG, ">>> onCreate()");
		super.onCreate();

		// Preferences:
		// - sets the default values
		// - register change listener
		// ---------------------------------------------------------------------
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		final SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		sharedPrefs.registerOnSharedPreferenceChangeListener(this);

		// Register asset file request handler.
		// ---------------------------------------------------------------------
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			DTalkNettyServerHandler.addRequestHandler("/",
					new AssetFileRequestHandler(this, "www", pInfo));
		} catch (NameNotFoundException e) {
			LOG.e(TAG, "Failed to register asset file request handler.", e);
		}

		// Initialize DTalkService.
		// ---------------------------------------------------------------------
		DTalkService.init(new JmDNSDTalkServiceConfiguration() {
			private volatile String mTargetName = null;

			@Override
			public String getType() {
				return "Telephony/1";
			}

			@Override
			public String getTargetName() {
				if (mTargetName == null) {
					mTargetName = android.os.Build.MODEL;
				}
				return sharedPrefs.getString(Constants.PREF_TARGET_NAME,
						mTargetName);
			}

			@Override
			public String getDeviceId() {
				String packageName = getApplicationInfo().packageName.replace(
						".", "-");
				return getHardwareAddress("") + "-" + packageName.trim();
			}

			@Override
			public boolean registerService() {
				return true;
			}

			@Override
			public boolean runServiceDiscovery() {
				return false;
			}

			@Override
			public String getWebPresenceURL() {
				return sharedPrefs.getString(Constants.PREF_WEB_PRESENCE_URL,
						null);
			}

			@Override
			public boolean isWebPresenceEnabled() {
				return sharedPrefs
						.getBoolean(Constants.PREF_WEB_PRESENCE, true);
			}

			@Override
			public InetSocketAddress getSocketAddress() {
				try {
					return new InetSocketAddress(getInetAddress(), getPort());
				} catch (UnknownHostException e) {
					LOG.e(TAG, e.getMessage(), e);
					return null;
				}
			}

			@Override
			public JmDNS getJmDNS() {
				try {
					return JmDNS();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

		});
	}

	@Override
	public void onTerminate() {
		LOG.v(TAG, ">>> onTerminate()");

		if (isServiceRunning(this, FreddoTelephonyService.class)) {
			stopService(new Intent(getBaseContext(),
					FreddoTelephonyService.class));
		}

		if (mJmDNS != null) {
			try {
				mJmDNS.close();
			} catch (IOException e) {
				// ignore;
			}
			mJmDNS = null;
		}

		while (multicastLock != null && multicastLock.isHeld()) {
			multicastLock.release();
		}

		super.onTerminate();
	}

	// Get InetAddress - the android way.
	protected InetAddress getInetAddress() throws UnknownHostException {
		LOG.d(TAG, ">>> getInetAddress");

		WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		LOG.d(TAG, "WIFI manager: %s", wifiManager);

		if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
			Log.d(TAG, "Wifi state is not enabled");
			return null;
		}

		WifiInfo connInfo = wifiManager.getConnectionInfo();
		int ipAddress = connInfo.getIpAddress();

		byte[] byteaddr = new byte[] { (byte) (ipAddress & 0xff),
				(byte) (ipAddress >> 8 & 0xff),
				(byte) (ipAddress >> 16 & 0xff),
				(byte) (ipAddress >> 24 & 0xff) };

		return InetAddress.getByAddress(byteaddr);
	}

	// Create JmDNS instance - the android way.
	private JmDNS JmDNS() throws IOException {
		LOG.v(TAG, ">> ensureJmDNS");

		if (multicastLock == null || !multicastLock.isHeld()) {
			WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			multicastLock = wifi.createMulticastLock(getApplicationInfo().name);
			multicastLock.setReferenceCounted(true);
			multicastLock.acquire();

			WifiInfo wifiinfo = wifi.getConnectionInfo();
			int intaddr = wifiinfo.getIpAddress();

			byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff),
					(byte) (intaddr >> 8 & 0xff),
					(byte) (intaddr >> 16 & 0xff),
					(byte) (intaddr >> 24 & 0xff) };
			InetAddress addr = InetAddress.getByAddress(byteaddr);

			mJmDNS = JmDNS.create(addr);
		}

		return mJmDNS;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		// If service is not running return.
		if (!isServiceRunning(this, FreddoTelephonyService.class)) {
			return;
		}

		// Target name has changed...
		if (key.equalsIgnoreCase(Constants.PREF_TARGET_NAME)) {
			DTalkService.getInstance().getConfiguration().getThreadPool()
					.execute(new Runnable() {
						@Override
						public void run() {
							DTalkService.getInstance().republishService();
						}
					});
		} else
		// WebPresence settings have changed...
		if (key.equalsIgnoreCase(Constants.PREF_WEB_PRESENCE)
				|| key.equalsIgnoreCase(Constants.PREF_WEB_PRESENCE_URL)) {
			final boolean webPresence = sharedPreferences.getBoolean(
					Constants.PREF_WEB_PRESENCE, true);
			DTalkService.getInstance().getConfiguration().getThreadPool()
					.execute(new Runnable() {
						@Override
						public void run() {
							DTalkService.getInstance().enableWebPresence(
									webPresence);
						}
					});
		}
	}

	// ---------------------------------------------------------------------
	// Utility methods
	// ---------------------------------------------------------------------

	/**
	 * Check if a service is running.
	 * 
	 * @param context
	 * @param serviceClass
	 * @return
	 */
	public static final boolean isServiceRunning(Context context,
			Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param context
	 * @param PackageName
	 * @return
	 */
	public static boolean isForeground(Context context, String PackageName) {
		// Get the Activity Manager
		ActivityManager manager = (ActivityManager) context
				.getSystemService(ACTIVITY_SERVICE);

		// Get a list of running tasks, we are only interested in the last one,
		// the top most so we give a 1 as parameter so we only get the topmost.
		@SuppressWarnings("deprecation")
		List<ActivityManager.RunningTaskInfo> task = manager.getRunningTasks(1);

		// Get the info we need for comparison.
		ComponentName componentInfo = task.get(0).topActivity;

		// Check if it matches our package name.
		if (componentInfo.getPackageName().equals(PackageName))
			return true;

		// If not then our app is not on the foreground.
		return false;
	}

}
