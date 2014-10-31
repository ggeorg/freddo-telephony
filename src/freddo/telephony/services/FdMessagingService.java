package freddo.telephony.services;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

public class FdMessagingService extends FdService {
	private static final String NAME = "dtalk.service.Messaging";

	private final Context mContext;

	public FdMessagingService(DTalkServiceContext context) {
		super(context, NAME);
		mContext = (Context) context;
	}

	@Override
	protected void onDisposed() {
		// TODO Auto-generated method stub
	}

	public void doSend(JSONObject request) throws DTalkException, JSONException {
		LOG.v(NAME, ">>> doSend: %s", request);

		JSONObject params = request.getJSONObject(DTalk.KEY_BODY_PARAMS);
		JSONArray phoneNo = params.getJSONArray("phoneNo");
		String message = params.getString("message");
		sendResponse(request,
				send(phoneNo.join(";").replace("\"", ""), message));
	}

	protected boolean send(String phoneNo, String message)
			throws DTalkException {
		if (!checkSupport()) {
			throw new DTalkException(DTalkException.INTERNAL_ERROR,
					"SMS feature is not supported on this device");
		}

		try {
			invokeIntent(phoneNo, message);
		} catch (Exception e) {
			throw new DTalkException(DTalkException.INTERNAL_ERROR,
					e.getMessage());
		}

		return true;
	}

	@SuppressLint("NewApi")
	private void invokeIntent(String phoneNo, String message) throws Exception {
		Intent sendIntent;
		if ("".equals(phoneNo)
				&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			String defaultSmsPackageName = Telephony.Sms
					.getDefaultSmsPackage(mContext);
			sendIntent = new Intent(Intent.ACTION_SEND);
			sendIntent.setType("text/plain");
			sendIntent.putExtra(Intent.EXTRA_TEXT, message);
			if (defaultSmsPackageName != null) {
				sendIntent.setPackage(defaultSmsPackageName);
			}
		} else {
			sendIntent = new Intent(Intent.ACTION_VIEW);
			sendIntent.setData(Uri.parse("smsto:" + Uri.encode(phoneNo)));
			sendIntent.putExtra("sms_body", message);
		}
		sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(sendIntent);
	}

	private boolean checkSupport() {
		return mContext.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_TELEPHONY);
	}

}
