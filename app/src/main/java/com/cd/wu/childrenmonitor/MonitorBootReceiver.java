package com.cd.wu.childrenmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by wu on 18-4-21.
 */

public class MonitorBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("MonitorBootReceiver", " onReceive(), intent: " + intent );

        Intent initIntent = new Intent("com.cd.wu.childrenmonitor.INIT_ACTION");
        context.startService(initIntent);
    }
}