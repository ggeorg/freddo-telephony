package freddo.telephony.services;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import freddo.dtalk.DTalk;
import freddo.dtalk.DTalkException;
import freddo.dtalk.DTalkServiceContext;
import freddo.dtalk.services.FdService;
import freddo.dtalk.util.LOG;

public class FdDialerService extends FdService {
	private static final String NAME = "dtalk.service.Dialer";

	private final Context mContext;

	public FdDialerService(DTalkServiceContext context) {
		super(context, NAME);
		mContext = (Context) context;
	}

	@Override
	protected void onDisposed() {
		// TODO Auto-generated method stub

	}

	public void doDial(JSONObject request) throws JSONException {
		LOG.v(NAME, ">>> doSend: %s", request);

		try {
			String phoneNo = request.getString(DTalk.KEY_BODY_PARAMS);
			sendResponse(request, dial(phoneNo));
		} catch (DTalkException e) {
			sendErrorResponse(request, e);
		} catch (JSONException e) {
			sendErrorResponse(request, DTalkException.INVALID_PARAMS,
					e.getMessage());
		}
	}

	private boolean dial(String phoneNo) throws DTalkException {
		if (!checkSupport()) {
			throw new DTalkException(DTalkException.INTERNAL_ERROR,
					"Dialer feature is not supported on this device");
		}

		try {
			Intent intent = new Intent(Intent.ACTION_DIAL);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setData(Uri.parse("tel:" + phoneNo));
			mContext.startActivity(intent);
		} catch (android.content.ActivityNotFoundException e) {
			throw new DTalkException(DTalkException.INTERNAL_ERROR, e.getMessage());
		}
		
		return true;
	}

	private boolean checkSupport() {
		return mContext.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_TELEPHONY);
	}

}
