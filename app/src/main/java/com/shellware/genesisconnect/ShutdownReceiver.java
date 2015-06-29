package com.shellware.genesisconnect;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ShutdownReceiver extends BroadcastReceiver{
	 
	private static final String RECEIVER_NAME = "ShutdownReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(RECEIVER_NAME, "onReceive");
		Intent service = new Intent();
		service.setComponent(new ComponentName(context, CanBusTripleService.class));
		context.stopService(service);
	}
}
