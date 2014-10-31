package freddo.telephony;

import java.util.concurrent.ExecutorService;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;
import freddo.dtalk.DTalkService;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdServiceMgr;
import freddo.dtalk.util.LOG;
import freddo.telephony.services.FdDialerService;
import freddo.telephony.services.FdMessagingService;

public class FreddoTelephonyService extends Service implements DTalkServiceContext {
	private static final String TAG = LOG.tag(FreddoTelephonyService.class);
	
	private volatile FdServiceMgr mServiceMgr = null;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LOG.v(TAG, ">>> onStartCommand");

		if (mServiceMgr == null) {
			if(FreddoTelephonyApp.isForeground(this, getApplicationInfo().packageName)) {
				Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
			}
			
			// Initialize and start DTalkService.
			DTalkService.getInstance().startup();
			
			// Create service manager & register services.
			mServiceMgr = new FdServiceMgr(this);
			mServiceMgr.registerService(new FdMessagingService(this));
			mServiceMgr.registerService(new FdDialerService(this));
		}

		// Using this return value, if the OS kills our Service it will recreate
		// it but the Intent that was sent to the Service isn’t re-delivered.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		LOG.v(TAG, ">>> onDestroy");
		
		if (mServiceMgr != null) {
			// Shutdown DTalkService.
			DTalkService.getInstance().shutdown();
			
			// Dispose service manager.
			mServiceMgr.dispose();
			mServiceMgr = null;
		}

		super.onDestroy();

		if(FreddoTelephonyApp.isForeground(this, getApplicationInfo().packageName)) {
			Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public ExecutorService getThreadPool() {
		return DTalkService.getInstance().getConfiguration().getThreadPool();
	}

	@Override
	public void runOnUiThread(Runnable r) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void assertBackgroundThread() {
		// TODO Auto-generated method stub
		
	}

}
